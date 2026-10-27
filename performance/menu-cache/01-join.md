# 1단계: 메뉴 목록 조회의 N+1 을 JOIN 한 번으로

실행 원본: [EC2](results/2026-10-26-ec2.md), [Mac](results/2026-10-26-mac.md)

## 1. 질문

기존 손님 메뉴 목록 조회(user-api `MenuService.getAllMenusByStoreId`, 커밋 `d20c593`)는 메뉴마다 이미지를 따로 조회했다. admin-api 의 같은 이름 메서드도 같은 모양이다.

```java
List<Menu> menus = menuRepository.findAllByStoreIdAndDeletedFalseOrderBySortOrder(storeId);  // 1번
menus.stream().map(menu -> {
    List<MenuImage> images = menuImageRepository.findByMenu(menu);                           // 메뉴마다 1번
    ...
```

- 요청당 쿼리 = 주점 1 + 메뉴 1 + 이미지 N. 메뉴 20개면 22개
- JPA 지연 로딩 N+1 이 아니라, 리포지토리를 루프에서 부르는 N+1 이다
- 연관관계가 `MenuImage → Menu` 단방향 `@ManyToOne` 뿐이라 `Menu` 쪽에서 이미지를 fetch join 할 수 없다

질문
- JOIN 한 번으로 바꿔도 응답이 기존과 같은가? (이미지 없는 메뉴, 이미지 여러 장, 정렬)
- 쿼리가 22개 → 2개가 되면 MySQL 한계가 얼마나 오르는가?

## 2. 바꾼 것

`MenuRepository` 에 JOIN 쿼리와, 결과 행을 메뉴별로 묶는 default 메서드를 넣었다. user(`DistributedCachedMenuDao.load`)와 admin(`MenuService.getAllMenusByStoreId`) 둘 다 이것을 쓴다.

```java
// 이미지가 아직 없는 메뉴도 보여야 하므로 LEFT JOIN (행: [Menu, MenuImage 또는 null])
@Query(value = """
    SELECT m, i
      FROM Menu m
      LEFT JOIN MenuImage i ON i.menu = m
     WHERE m.storeId = :storeId
       AND m.deleted = false
     ORDER BY m.sortOrder, i.id
    """)
List<Object[]> findAllWithImageRowsByStoreId(@Param("storeId") Long storeId);

default Map<Menu, List<MenuImage>> findAllWithImagesByStoreId(Long storeId) {
    return findAllWithImageRowsByStoreId(storeId).stream()
        .collect(Collectors.groupingBy(row -> (Menu)row[0], LinkedHashMap::new,
            Collectors.flatMapping(row -> Stream.ofNullable((MenuImage)row[1]), Collectors.toList())));
}
```

- INNER JOIN 이 아니라 LEFT JOIN: admin 에서 메뉴 생성과 이미지 업로드가 따로라, 이미지가 아직 없는 메뉴가 있을 수 있다. INNER 면 손님 목록에서 사라진다
- `LinkedHashMap` 으로 `sortOrder` 순서를 지키고, 이미지가 없는 메뉴는 빈 목록이 된다
- `ORDER BY i.id`: 기존 `findByMenu` 가 돌려주던 이미지 순서(id 순)와 맞춘다

```
기준  주점 SELECT → 메뉴 SELECT → 이미지 SELECT × 20   (+ 트랜잭션 설정 문장 약 5개)
1단계 주점 SELECT → 메뉴 LEFT JOIN 이미지 SELECT 1번
```

이 단계의 user-api(`user-tobe.jar`)는 조회 트랜잭션도 없다(3단계에서 넣은 변경, [3단계 2절](03-cache.md#2-바꾼-것)). 기준 코드는 `@Transactional(readOnly = true)` 라 요청마다 `SET autocommit`, `SET SESSION TRANSACTION READ ONLY / READ WRITE`, `COMMIT` 이 더 나갔다. 이 단계의 차이에는 그것도 섞여 있다. 크기는 4절에서 나눠 본다.

## 3. 실행 명령

```bash
performance/menu-cache/run.sh verify
RATES="100 200 400"  performance/menu-cache/run.sh bench nplus1
RATES="200 500 1000" performance/menu-cache/run.sh bench join     # 캐시 끔(ttl 0s), 인덱스 지운 상태
```

## 4. 관찰 결과

**응답은 기존과 같다.** 기존 jar 와 바꾼 jar 를 동시에 띄워 200개 주점 응답을 비교했다. user·admin 모두 200/200 같았다(EC2, Mac 둘 다). admin 목록 200회의 메뉴 관련 쿼리는 4,200(menus 200 + menu_images 4,000) → 200 이다.

**요청당 쿼리 시간** (EC2, `performance_schema` 의 누적 실행 시간 ÷ 요청 수. 기준은 100 req/s, 1단계는 200 req/s)

| 문장 | 기준 N+1 | 1단계 JOIN |
|---|---:|---:|
| 이미지 SELECT × 20 (`menu_images`) | 2.02ms | - |
| 메뉴 SELECT / 메뉴·이미지 JOIN (`menus`) | 1.88ms | 2.09ms |
| 주점 SELECT (`stores`) | 0.12ms | 0.14ms |
| 그 밖의 문장 (트랜잭션 설정 등) | 0.23ms | 0.03ms (admin 쪽) |
| 합계 | 4.25ms | 2.26ms |

- 줄어든 약 2ms 의 대부분은 이미지 SELECT 20번(2.02ms)이다. 트랜잭션 설정 문장(요청당 약 5개)은 0.2ms 정도였다
- `menus` 는 두 단계 모두 요청마다 약 4,000행을 읽었다(메뉴 테이블 전체 스캔). 이것이 2단계의 주제다

**같은 부하에서 (EC2)**

| 목표 req/s | 단계 | 처리 req/s | p50 | p95 | p99 | 못 보낸 요청 | 요청당 쿼리 | MySQL CPU | user-api CPU |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 100 | N+1 | 100 | 9.3ms | 10.0ms | 11.4ms | 0 | 22.01 | 0.47 | 0.48 |
| 200 | N+1 | 187 | 1,492ms | 2,502ms | 3,157ms | 285 | 22.00 | 0.99 | 0.84 |
| 400 | N+1 | 187 | 10,625ms | 10,749ms | 11,704ms | 10,778 | 22.00 | 0.99 | 0.80 |
| 200 | JOIN | 200 | 3.5ms | 4.2ms | 4.7ms | 0 | 2.01 | 0.46 | 0.43 |
| 500 | JOIN | 499 | 74.2ms | 129.5ms | 160.6ms | 0 | 1.84 | 0.99 | 0.54 |
| 1000 | JOIN | 658 | 2,893ms | 3,170ms | 3,323ms | 18,517 | 1.62 | 0.99 | 0.66 |

- N+1 은 200 req/s 에서 이미 무너졌다. MySQL 이 1코어를 다 쓰고 187 req/s 에서 멈췄다
- JOIN 은 200 req/s 에서 MySQL 0.46코어로 여유가 있고, 500 에서 1코어가 찼다
- p95 200ms 안에서 처리한 최대 부하: 100 → 500
- JOIN 500·1000 은 캐시를 껐는데도 히트율이 16%·38% 로 찍혔다. TTL 0s 라도 같은 주점에 동시에 온 요청은 로딩 한 번을 나눠 받기 때문이다(Caffeine 키 단위 로딩, [3단계 4절](03-cache.md#4-관찰-결과)). 과부하 구간의 JOIN 수치는 그만큼 유리하다

Mac 에서도 순서는 같았다. N+1 은 200 req/s 를 넣어도 116 req/s, JOIN 은 200 req/s 를 p95 7.4ms 로 처리했다.

## 5. 보장 범위와 해결하지 못한 문제

- 응답이 같다는 비교는 시드 데이터(이미지 0·1·2장) 기준이다. 삭제된 메뉴 제외는 `MenuRepositoryTest` 로만 확인했다
- 이미지 순서를 위해 `ORDER BY i.id` 를 넣어 실행 계획에 `Using temporary; Using filesort` 가 남는다. 20행 정렬이라 무시했다
- 이 단계의 차이에는 조회 트랜잭션 제거가 섞여 있다. 둘을 나눠 재지 않고, 쿼리 시간으로 크기만 나눠 봤다
- 결과를 `Object[]` 행으로 받아 Java 에서 묶는다. `Menu` 에 `@OneToMany images` 를 두고 fetch join 하는 방법은 두 앱이 함께 쓰는 엔티티를 바꿔야 해서 택하지 않았다

## 6. 다음에 볼 것

- 남은 요청당 약 2ms 의 대부분이 `menus` 전체 스캔이다 → 2단계 인덱스
