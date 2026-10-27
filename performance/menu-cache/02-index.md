# 2단계: menus 에 (store_id, deleted, sort_order) 인덱스

실행 원본: [EC2](results/2026-10-26-ec2.md), [Mac](results/2026-10-26-mac.md)

## 1. 질문

1단계 JOIN 쿼리는 요청마다 `menus` 를 약 4,000행 읽었다. `menus.store_id` 에 인덱스가 없었기 때문이다.

- `Menu.storeId` 는 연관관계가 아니라 `Long` 컬럼이라 FK 가 없고, InnoDB 가 FK 때문에 자동으로 만드는 인덱스도 없다
- 다른 쪽은 이미 인덱스가 있다: `menu_images.menu_id` 는 FK 인덱스, `stores.public_code` 는 unique 인덱스

질문
- 인덱스로 요청당 읽는 행과 MySQL CPU 가 얼마나 줄고, 한계가 얼마나 오르는가?
- 운영 DB 에 별도 마이그레이션 없이 만들 수 있는가?

## 2. 바꾼 것

```java
// 주점별 메뉴 조회(WHERE store_id = ? AND deleted = ? ORDER BY sort_order)가 전체 스캔·filesort 없이 끝나게 한다
@Table(name = "menus", indexes = @Index(name = "idx_menus_store_id_deleted_sort_order",
    columnList = "store_id, deleted, sort_order"))
public class Menu extends BaseTimeEntity {
```

- 같다(`=`)로 거르는 컬럼 둘을 앞에, 정렬 컬럼을 뒤에 뒀다. `deleted` 는 값 종류가 적지만 앞에 같다 조건으로 있어야 `sort_order` 순서로 바로 읽힌다
- 두 앱 모두 `ddl-auto: update` 라 배포 때 Hibernate 가 만든다. EC2·Mac 모두 앱 시작 후 생성된 것을 확인했다(`idx_menus_store_id_deleted_sort_order(store_id, deleted, sort_order)`)
- 측정에서는 `run.sh` 가 단계에 맞게 인덱스를 지우거나 다시 만든다(앱이 시작할 때 만들기 때문)

**실행 계획** (Mac, 같은 시드 데이터)

```
EXPLAIN SELECT m.*, i.* FROM menus m LEFT JOIN menu_images i ON i.menu_id = m.id
 WHERE m.store_id = 7 AND m.deleted = 0 ORDER BY m.sort_order, i.id

인덱스 없음  m: type ALL, rows 3978, filtered 5.00, Using where; Using temporary; Using filesort
            i: type ref, key FK(menu_id), rows 1
인덱스 있음  m: type ref, key idx_menus_store_id_deleted_sort_order, ref const,const, rows 20, Using temporary; Using filesort
            i: type ref, key FK(menu_id), rows 1
```

`Using filesort` 는 두 번째 테이블 컬럼(`i.id`)으로도 정렬해서 남는다. 20행 정렬이다.

## 3. 실행 명령

```bash
RATES="200 1000 2000 3000 4000" performance/menu-cache/run.sh bench join-idx   # 캐시 끔(ttl 0s), 인덱스 있음
```

## 4. 관찰 결과

**읽는 행이 4,042 → 62 로 줄었다 (EC2, 200 req/s)**

| | 1단계 JOIN | 2단계 + 인덱스 |
|---|---:|---:|
| 요청당 읽은 행 | 4,042 | 62 |
| 요청당 쿼리 시간: `menus` (JOIN) | 2.09ms | 0.32ms |
| 요청당 쿼리 시간: `stores` | 0.14ms | 0.15ms |
| MySQL CPU | 0.46코어 | 0.11코어 |
| p95 | 4.2ms | 2.4ms |

**속도별 (EC2)**

| 목표 req/s | 처리 req/s | p50 | p95 | p99 | 못 보낸 요청 | 요청당 읽은 행 | MySQL CPU | user-api CPU |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 200 | 200 | 1.8ms | 2.4ms | 2.8ms | 0 | 62 | 0.11 | 0.41 |
| 1000 | 998 | 1.6ms | 1.8ms | 2.6ms | 0 | 62 | 0.50 | 0.92 |
| 2000 | 1,995 | 13.4ms | 57.6ms | 74.5ms | 0 | 56 | 0.99 | 2.00 |
| 3000 | 2,356 | 799ms | 937ms | 1,067ms | 36,353 | 44 | 0.99 | 2.21 |
| 4000 | 2,333 | 822ms | 976ms | 1,100ms | 97,755 | 44 | 0.99 | 2.25 |

- p95 200ms 안에서 처리한 최대 부하: 500 → 2,000 (약 4배). 2,000 에서 MySQL 이 1코어를 다 썼고, 3,000 을 넣으면 2,356 에서 멈췄다
- 요청당 MySQL CPU: 2.3ms (200 req/s) → 0.50ms (1,000 req/s)
- 3,000·4,000 에서 요청당 읽은 행이 44 로 줄어든 것은 동시에 온 같은 주점 요청이 로딩을 나눠 받아서다(히트율 29.7%·29.6%)

Mac 에서도 한계가 비슷하게 올랐다. 1,000 req/s 를 p95 4.6ms 로 처리했고, 3,000 을 넣으면 2,205 에서 멈췄다.

## 5. 보장 범위와 해결하지 못한 문제

- 인덱스 이름과 컬럼이 엔티티(`Menu`)와 `run.sh` 두 곳에 적혀 있다. 한쪽만 바꾸면 측정이 어긋난다
- 운영 테이블에 인덱스를 만드는 동안의 잠금은 확인하지 않았다. MySQL 8 의 보조 인덱스 생성은 온라인 DDL 이고, `menus` 는 작은 테이블이다
- `Using temporary; Using filesort` 가 남는다(2절)

## 6. 다음에 볼 것

- 남은 요청당 쿼리 시간은 `menus` 0.32ms + `stores` 0.15ms 다. 거의 바뀌지 않는 같은 데이터를 요청마다 다시 읽는다 → 3단계 캐시
