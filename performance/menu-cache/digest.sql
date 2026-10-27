-- 측정 구간 동안 테이블별 쿼리 수, 읽은 행 수, 누적 실행 시간(ms). admin(로그인·토글) 쿼리도 같은 스키마라 섞이지만 초당 1건 수준
-- 메뉴·이미지 JOIN 쿼리는 FROM `menus` 로 시작하므로 menus 로 센다
SELECT CASE
         WHEN DIGEST_TEXT LIKE '%FROM `menu_images`%' THEN 'menu_images'
         WHEN DIGEST_TEXT LIKE '%FROM `menus`%' THEN 'menus'
         WHEN DIGEST_TEXT LIKE '%FROM `stores`%' THEN 'stores'
         ELSE 'other' END AS tbl,
       SUM(COUNT_STAR) AS calls,
       SUM(SUM_ROWS_EXAMINED) AS rows_examined,
       ROUND(SUM(SUM_TIMER_WAIT) / 1e9, 1) AS total_ms
FROM performance_schema.events_statements_summary_by_digest
WHERE SCHEMA_NAME = 'nowait_lab'
GROUP BY tbl
ORDER BY tbl;
