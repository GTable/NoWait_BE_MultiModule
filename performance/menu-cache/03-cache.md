# 3단계: 로컬 캐시 + Redis Pub/Sub 무효화

실행 원본: [EC2](results/2026-10-26-ec2.md), [Mac](results/2026-10-26-mac.md)

## 1. 질문

2단계 뒤에도 손님 메뉴 조회는 요청마다 DB 에 갔다(주점 SELECT + 메뉴·이미지 JOIN). 2,000 req/s 에서 MySQL 이 1코어를 다 썼다. 메뉴는 거의 바뀌지 않는데 같은 데이터를 매번 다시 읽는다.

캐시를 붙이는 데 걸리는 제약
- **쓰는 서버와 읽는 서버가 다르다.** 메뉴·품절은 admin-api 가 바꾸고 user-api 가 읽는다. user-api 에 로컬 캐시만 두면 admin-api 의 변경을 모른다
- **품절은 바로 보여야 한다.** 짧은 TTL 은 히트율을 떨어뜨리고, 긴 TTL 은 품절된 메뉴를 그동안 보여 준다
- **Redis 는 웨이팅 대기열과 함께 쓴다.** 조회마다 Redis 를 거치는 전역 캐시는 대기열과 자원을 나눠 쓴다

채널톡 기술 블로그의 Distributed Cache 도입기([1](https://tech.channel.io/kr/articles/tech-distributed-cache-1-67a392c5), [2](https://docs.channel.io/tech-blog/ko/articles/tech-distributed-cache-2-f77c0868), [3](https://docs.channel.io/tech-blog/ko/articles/tech-cache-rdb-4cc0bbf4))는 같은 문제를 로컬 캐시 + Redis Pub/Sub 키 무효화로 풀었다. 짧은 TTL 의 로컬 캐시와 Redis 전역 캐시(단일 노드 CPU 70%, 롤백)를 거친 뒤다. 그 구조를 그대로 가져왔다.

질문
- 캐시를 켜면 DB 쿼리와 MySQL CPU 가 얼마나 줄고, 한계가 얼마나 오르는가?
- 부하 중에 품절을 바꾸면 손님 응답에 바로 반영되는가?
- 무효화 직후 같은 주점 요청이 몰리면 DB 로 새는가?

## 2. 바꾼 것

```
[조회]  손님 → user-api → 로컬 캐시(Caffeine) 히트 → 응답
                       └ 미스 → MySQL 에서 읽어 캐시에 넣음 (같은 키 동시 미스는 한 번만 읽음)

[변경]  점주 → admin-api → MySQL 커밋
                       └ 커밋 후 → Redis PUBLISH cache:menus "INVALIDATE-{storeId}"
                                     └ 구독 중인 모든 user-api 가 그 주점 키만 지움
```

| 파일 | 하는 일 |
|---|---|
| `domain-redis` `DistributedCachedDao` | Caffeine 캐시 + 채널 구독. `get(key)` = `cache.get(key, this::load)`, 메시지를 받으면 키를 지운다 |
| `domain-redis` `CacheInvalidationPublisher` | `INVALIDATE-{key}` / `CLEAR` 를 커밋 후 발행 |
| user-api `DistributedCachedMenuDao` | 키 = storeId, 값 = 주점 메뉴 목록. TTL 1일(`nowait.cache.menu.ttl`, 0s 면 끔), 최대 1,000개 |
| admin-api `MenuService`·`MenuImageService` | 무효화 7곳: 메뉴 생성·수정·순서 변경·삭제·품절 토글, 이미지 업로드·삭제 |

- 채널톡 구현처럼 Redis 에는 데이터가 아니라 키만 보낸다. 캐시에 값을 넣는(put) 경로는 없고, 지운 뒤 다음 조회가 DB 에서 다시 읽는다
- Redis 에는 메뉴가 바뀔 때만 메시지가 오간다. 조회 경로는 Redis 를 거치지 않는다

```java
public V get(String key) {
    return cache.get(key, this::load);
}

@Override
public void onMessage(Message message, byte[] pattern) {
    String body = new String(message.getBody(), StandardCharsets.UTF_8);
    if (CacheInvalidationPublisher.CLEAR.equals(body)) {
        cache.invalidateAll();
    } else if (body.startsWith(CacheInvalidationPublisher.INVALIDATE_PREFIX)) {
        cache.invalidate(body.substring(CacheInvalidationPublisher.INVALIDATE_PREFIX.length()));
    }
}
```

**정합성을 지키는 장치 세 가지**

1. **커밋 후에만 발행한다.** 커밋 전에 지우면, 그사이 들어온 조회가 커밋 전 값을 다시 읽어 TTL 동안 캐시한다. `TransactionSynchronization.afterCommit` 에서 발행한다
2. **재구독하면 전부 비운다.** Pub/Sub 은 연결이 끊긴 동안의 메시지를 다시 주지 않는다. `onChannelSubscribed`(처음 구독할 때와 끊겼다 다시 구독할 때 모두 불린다)에서 `invalidateAll()`
3. **조회 트랜잭션을 없앴다.** user-api 조회에 `@Transactional(readOnly = true)` 가 있으면 주점 조회 때 잡힌 스냅샷(REPEATABLE READ) 안에서 캐시 미스 로딩이 일어난다. 무효화 직후에도 커밋 전 메뉴를 읽어 하루 동안 캐시할 수 있다. 쿼리마다 따로 커밋된 최신 값을 읽게 했다

AS-IS(2단계)와 TO-BE(3단계)는 같은 jar 에서 TTL 설정만 다르다(`0s` / `1d`). 두 단계의 차이는 캐시 하나다.

## 3. 실행 명령

```bash
RATES="200 1000 2000 3000 4000" performance/menu-cache/run.sh bench join-idx   # AS-IS: 캐시 끔
RATES="200 1000 2000 4000 6000" performance/menu-cache/run.sh bench cache      # TO-BE
performance/menu-cache/run.sh delay   # 400 req/s 부하 중 품절 토글 30회 → 손님 응답 반영 확인 (probe.sh)
```

## 4. 관찰 결과

**DB 부하 (EC2, 1,000 req/s 60초)**

| | AS-IS | TO-BE | 감소 |
|---|---:|---:|---:|
| 메뉴 쿼리 (`menus`, 메뉴·이미지 JOIN) | 59,808 | 123 | 99.8% |
| 주점 쿼리 (`stores`) | 59,982 | 59,960 | 0% (캐시 대상 아님) |
| RDB 쿼리 합계 | 119,790 | 60,083 | 49.8% (요청당 2 → 1) |
| 요청당 읽은 행 | 62 | 1 | 98% |
| 요청당 쿼리 시간 | 0.45ms | 0.13ms | 71% |
| MySQL CPU | 0.50코어 | 0.15코어 | 70% |
| 히트율 | - | 99.9% | |
| p95 | 1.8ms | 1.1ms | |

- TO-BE 에 남은 메뉴 쿼리 123건은 초당 1건 품절 토글 때문이다. 점주 쪽 토글 처리가 메뉴를 읽은 약 60건과, 무효화된 키를 다시 채운 약 60건이다
- RDB 쿼리 합계는 절반만 줄었다. 요청마다 나가는 주점 조회를 캐시하지 않았기 때문이다

**속도별 (EC2)**

| 목표 req/s | AS-IS 처리 | AS-IS p95 | AS-IS MySQL CPU | TO-BE 처리 | TO-BE p95 | TO-BE MySQL CPU | TO-BE user-api CPU |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 200 | 200 | 2.4ms | 0.11 | 200 | 1.6ms | 0.04 | 0.36 |
| 1000 | 998 | 1.8ms | 0.50 | 998 | 1.1ms | 0.15 | 0.69 |
| 2000 | 1,995 | 57.6ms | 0.99 | 1,996 | 1.3ms | 0.31 | 1.35 |
| 3000 | 2,356 | 937ms | 0.99 | - | - | - | - |
| 4000 | 2,333 | 976ms | 0.99 | 3,992 | 2.2ms | 0.59 | 2.62 |
| 6000 | - | - | - | 5,894 | 256ms | 0.88 | 3.89 |

- p95 200ms 안에서 처리한 최대 부하: 2,000 → 4,000 (2배). 최대 처리량: 2,356 → 5,894
- 같은 2,000 req/s 에서 p95 57.6ms → 1.3ms
- 요청당 MySQL CPU: 0.50ms → 0.15ms (1,000 req/s)
- AS-IS 는 과부하 구간에서 같은 주점 동시 요청이 로딩을 나눠 받아(2,000 에서 히트율 10.2%, 3,000 이상 약 30%) 실제보다 조금 유리하게 측정됐다. 캐시 효과는 표보다 조금 크다

**무효화 직후 몰려도 DB 로 새지 않았다** (EC2, 초당 1건 품절 토글, 60초 동안의 메뉴 쿼리)

| 목표 req/s | 200 | 1,000 | 2,000 | 4,000 | 6,000 |
|---|---:|---:|---:|---:|---:|
| 메뉴 쿼리 | 123 | 123 | 122 | 122 | 123 |

- 부하가 30배가 돼도 메뉴 쿼리 수가 그대로다. 토글은 인기 주점에 몰리게 보냈으니 무효화 직후 같은 주점 요청이 실제로 몰렸다. 그래도 무효화 1번에 다시 채우는 쿼리는 1번이다
- `cache.get(key, loader)` 는 같은 키를 로딩하는 동안 다른 요청을 기다리게 하고 그 결과를 나눠 준다. 별도 락 없이 서버 한 대 안에서 미스가 하나로 모인다

**품절 반영** (EC2, 400 req/s 조회 부하 중 품절 토글 30회. 목록 80% · 단건 20%)

| 토글 응답 p50 / p95 | 토글 응답 후 바뀐 값 확인 p50 / p95 / 최대 | 옛 값을 받은 조회 |
|---|---|---:|
| 17ms / 18ms | 9ms / 10ms / 11ms | 0 / 30회 |

- 30번 모두 토글 응답 직후 첫 조회부터 바뀐 값이 나왔다. "바뀐 값 확인" 9ms 는 조회 한 번에 걸린 시간이다
- 무효화는 커밋 후, HTTP 응답 전에 발행된다. 같은 서브넷의 Pub/Sub 전달이 응답보다 먼저 끝났다

**재구독** (Mac 로컬 E2E, 2026-10-26, 원본 기록 없음. 콘솔 출력으로만 확인)
- Redis 에서 `CLIENT KILL TYPE pubsub` 으로 구독 연결을 끊자 약 200ms 뒤 다시 구독했고(`PUBSUB NUMSUB cache:menus` = 1), 바로 다음 조회는 캐시 미스였다(전부 비워짐)

**다음 한계**: 6,000 req/s 에서 user-api CPU 가 3.89/4 코어였다. MySQL 은 0.88코어로, 남은 쿼리는 주점 조회뿐이다. k6 인스턴스 CPU 는 재지 않아 못 보낸 요청 5,180건이 부하 발생기 쪽 한계인지는 가르지 못했다.

## 5. 보장 범위와 해결하지 못한 문제

- 결과적 일관성이다. 커밋과 무효화 메시지 처리 사이(같은 서브넷에서 수 ms)에는 옛 값이 나갈 수 있다. 측정에서는 한 번도 보이지 않았다(4절)
- **무효화 메시지는 유실될 수 있다.**
  - 연결이 끊긴 동안 놓친 메시지는 재구독 때 전부 비워 메운다
  - 커밋 때 Redis 가 죽어 발행 자체가 실패하면 옛 값이 TTL(1일)까지 남는다. 에러 로그를 남기고 `redis-cli PUBLISH cache:menus CLEAR` 로 수동 복구한다. 재시도·아웃박스는 넣지 않았다. TTL 을 없애지 않은 이유이기도 하다(채널톡도 같은 이유로 남겼다)
- **미스가 하나로 모이는 것은 서버 한 대 안에서다.** user-api 가 N대면 무효화 한 번에 DB 조회도 N번이다. 분산 락은 넣지 않았다(미스마다 Redis 왕복이 늘고, Redis 를 대기열과 함께 쓰는 문제가 돌아온다)
- **로딩하는 동안 같은 주점 요청은 기다린다.** 옛 값을 주면서 다시 읽는 방식(refresh)은 품절 직후 품절 전 메뉴를 보여 주므로 택하지 않았다. DB 가 느려지면 그 주점 요청도 함께 느려진다
- `CLEAR` 나 배포 직후에는 모든 키가 비어 있다. 주점 200개면 서버마다 최대 200번 다시 읽는다
- 주점 조회는 캐시하지 않았다. 캐시를 켠 뒤 MySQL 부하의 거의 전부다
- user-api 한 대로만 쟀다. 여러 대에 무효화가 퍼지는 것과, 그때의 반영 시간은 재지 않았다

## 6. 다음에 볼 것

- 주점 조회 캐시: 같은 구조(키 = publicCode)로 요청당 남은 쿼리 1개를 없애기. 주점 정보 변경 지점에 무효화를 넣어야 한다
- user-api 를 두 대 이상 띄워 무효화가 모든 서버에 퍼지는지, 반영 시간이 어떻게 되는지
- 발행 실패 대비: 실패한 무효화를 재시도하거나, 커밋과 함께 남겨 두고(아웃박스) 나중에 다시 보내기
- k6 인스턴스 CPU 를 함께 기록해, 6,000 req/s 의 한계가 앱 서버인지 부하 발생기인지 가르기
