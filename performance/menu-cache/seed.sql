-- 목업: 학과 5, 주점 @stores, 주점당 메뉴 20. 메뉴 id = (store_id - 1) * 20 + n, 공개 코드 = LOAD + 8자리 store_id
-- 이미지: 메뉴당 1장, 단 2번 메뉴는 2장·20번 메뉴는 없음 (JOIN 결과를 메뉴별로 묶는지, 이미지 없는 메뉴가 빠지지 않는지 확인용)
-- 실행 전에 SET @stores = <주점 수>;
SET SESSION cte_max_recursion_depth = 1000000;
DELETE FROM menu_images;
DELETE FROM menus;
DELETE FROM stores;
DELETE FROM departments;

INSERT INTO departments (id, name)
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 5)
SELECT i, CONCAT('dept-', i) FROM n;

INSERT INTO stores (store_id, created_at, deleted, department_id, is_active, name, public_code, location, description)
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < @stores)
SELECT i, NOW(), 0, i % 5 + 1, 1, CONCAT('store-', i), CONCAT('LOAD', LPAD(i, 8, '0')), 'location', 'description' FROM n;

INSERT INTO menus (id, created_at, admin_display_name, deleted, description, is_sold_out, name, price, sort_order, store_id)
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < @stores * 20)
SELECT i, NOW(), CONCAT('menu-', i), 0, 'description', 0, CONCAT('menu-', i), 1000 * ((i - 1) % 20 + 1), (i - 1) % 20 + 1, (i - 1) DIV 20 + 1 FROM n;

INSERT INTO menu_images (id, created_at, file_key, image_url, menu_id)
SELECT id, NOW(), CONCAT('menu/', id), CONCAT('https://img.example.com/menu/', id, '.png'), id FROM menus WHERE sort_order <> 20
UNION ALL
SELECT id + @stores * 20, NOW(), CONCAT('menu/', id, '-2'), CONCAT('https://img.example.com/menu/', id, '-2.png'), id FROM menus WHERE sort_order = 2;
