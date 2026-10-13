-- 기준 데이터 시드(ADR-025): 스포츠 용품 카테고리, 옵션 그룹·값(코드 포함), 카테고리별 옵션 그룹 지정.
-- 이미 같은 코드·이름·값이 있으면 건너뛰므로(INSERT IGNORE) 기존 데이터는 바뀌지 않는다.
-- 용도(SPEC·SKU)는 카테고리별로 정한 초기값이며, 이후 PUT /products/categories/{id}/option-groups로 바꿀 수 있다.

-- 카테고리
INSERT IGNORE INTO category (parent_category_id, category_code, name, depth, sort_order) VALUES (NULL, 'RKT', '라켓', 1, 0);
INSERT IGNORE INTO category (parent_category_id, category_code, name, depth, sort_order) VALUES (NULL, 'SHC', '셔틀콕', 1, 1);
INSERT IGNORE INTO category (parent_category_id, category_code, name, depth, sort_order) VALUES (NULL, 'SHO', '신발', 1, 2);
INSERT IGNORE INTO category (parent_category_id, category_code, name, depth, sort_order) VALUES (NULL, 'APR', '의류', 1, 3);
INSERT IGNORE INTO category (parent_category_id, category_code, name, depth, sort_order) VALUES (NULL, 'BAG', '가방', 1, 4);
INSERT IGNORE INTO category (parent_category_id, category_code, name, depth, sort_order) VALUES (NULL, 'STR', '스트링', 1, 5);
INSERT IGNORE INTO category (parent_category_id, category_code, name, depth, sort_order) VALUES (NULL, 'GRP', '그립', 1, 6);
INSERT IGNORE INTO category (parent_category_id, category_code, name, depth, sort_order) VALUES (NULL, 'PRT', '보호대', 1, 7);

-- 옵션 그룹
INSERT IGNORE INTO option_group (name) VALUES ('무게 등급');
INSERT IGNORE INTO option_group (name) VALUES ('그립 사이즈');
INSERT IGNORE INTO option_group (name) VALUES ('색상');
INSERT IGNORE INTO option_group (name) VALUES ('밸런스');
INSERT IGNORE INTO option_group (name) VALUES ('샤프트 경도');
INSERT IGNORE INTO option_group (name) VALUES ('셔틀콕 소재');
INSERT IGNORE INTO option_group (name) VALUES ('셔틀콕 속도');
INSERT IGNORE INTO option_group (name) VALUES ('신발 사이즈');
INSERT IGNORE INTO option_group (name) VALUES ('문자 사이즈');
INSERT IGNORE INTO option_group (name) VALUES ('가방 종류');

-- 옵션 값
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '2U', '2U', 0 FROM option_group WHERE name = '무게 등급';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '3U', '3U', 1 FROM option_group WHERE name = '무게 등급';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '4U', '4U', 2 FROM option_group WHERE name = '무게 등급';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '5U', '5U', 3 FROM option_group WHERE name = '무게 등급';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'G4', 'G4', 0 FROM option_group WHERE name = '그립 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'G5', 'G5', 1 FROM option_group WHERE name = '그립 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'G6', 'G6', 2 FROM option_group WHERE name = '그립 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'BLK', '검정', 0 FROM option_group WHERE name = '색상';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'WHT', '흰색', 1 FROM option_group WHERE name = '색상';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'RED', '빨강', 2 FROM option_group WHERE name = '색상';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'BLU', '파랑', 3 FROM option_group WHERE name = '색상';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'GRN', '초록', 4 FROM option_group WHERE name = '색상';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'YEL', '노랑', 5 FROM option_group WHERE name = '색상';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'NVY', '네이비', 6 FROM option_group WHERE name = '색상';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'GRY', '회색', 7 FROM option_group WHERE name = '색상';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'HEAD', '헤드 헤비', 0 FROM option_group WHERE name = '밸런스';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'EVEN', '이븐', 1 FROM option_group WHERE name = '밸런스';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'LIGHT', '헤드 라이트', 2 FROM option_group WHERE name = '밸런스';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'SOFT', '소프트', 0 FROM option_group WHERE name = '샤프트 경도';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'MED', '미디엄', 1 FROM option_group WHERE name = '샤프트 경도';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'STIFF', '스티프', 2 FROM option_group WHERE name = '샤프트 경도';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'XSTF', '엑스트라 스티프', 3 FROM option_group WHERE name = '샤프트 경도';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'FEATHER', '깃털', 0 FROM option_group WHERE name = '셔틀콕 소재';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'NYLON', '나일론', 1 FROM option_group WHERE name = '셔틀콕 소재';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '75', '75', 0 FROM option_group WHERE name = '셔틀콕 속도';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '76', '76', 1 FROM option_group WHERE name = '셔틀콕 속도';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '77', '77', 2 FROM option_group WHERE name = '셔틀콕 속도';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '78', '78', 3 FROM option_group WHERE name = '셔틀콕 속도';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '230', '230mm', 0 FROM option_group WHERE name = '신발 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '240', '240mm', 1 FROM option_group WHERE name = '신발 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '250', '250mm', 2 FROM option_group WHERE name = '신발 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '260', '260mm', 3 FROM option_group WHERE name = '신발 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '270', '270mm', 4 FROM option_group WHERE name = '신발 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '280', '280mm', 5 FROM option_group WHERE name = '신발 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, '290', '290mm', 6 FROM option_group WHERE name = '신발 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'XS', 'XS', 0 FROM option_group WHERE name = '문자 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'S', 'S', 1 FROM option_group WHERE name = '문자 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'M', 'M', 2 FROM option_group WHERE name = '문자 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'L', 'L', 3 FROM option_group WHERE name = '문자 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'XL', 'XL', 4 FROM option_group WHERE name = '문자 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'XXL', 'XXL', 5 FROM option_group WHERE name = '문자 사이즈';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'BACKPACK', '백팩', 0 FROM option_group WHERE name = '가방 종류';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'RACKETBAG', '라켓백', 1 FROM option_group WHERE name = '가방 종류';
INSERT IGNORE INTO option_value (option_group_id, option_code, value, sort_order) SELECT option_group_id, 'BOSTON', '보스턴백', 2 FROM option_group WHERE name = '가방 종류';

-- 카테고리별 옵션 그룹 지정
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 0 FROM category c, option_group g WHERE c.category_code = 'RKT' AND g.name = '무게 등급';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 1 FROM category c, option_group g WHERE c.category_code = 'RKT' AND g.name = '그립 사이즈';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 2 FROM category c, option_group g WHERE c.category_code = 'RKT' AND g.name = '색상';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SPEC', 3 FROM category c, option_group g WHERE c.category_code = 'RKT' AND g.name = '밸런스';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SPEC', 4 FROM category c, option_group g WHERE c.category_code = 'RKT' AND g.name = '샤프트 경도';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SPEC', 0 FROM category c, option_group g WHERE c.category_code = 'SHC' AND g.name = '셔틀콕 소재';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 1 FROM category c, option_group g WHERE c.category_code = 'SHC' AND g.name = '셔틀콕 속도';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 0 FROM category c, option_group g WHERE c.category_code = 'SHO' AND g.name = '신발 사이즈';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 1 FROM category c, option_group g WHERE c.category_code = 'SHO' AND g.name = '색상';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 0 FROM category c, option_group g WHERE c.category_code = 'APR' AND g.name = '문자 사이즈';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 1 FROM category c, option_group g WHERE c.category_code = 'APR' AND g.name = '색상';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SPEC', 0 FROM category c, option_group g WHERE c.category_code = 'BAG' AND g.name = '가방 종류';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 1 FROM category c, option_group g WHERE c.category_code = 'BAG' AND g.name = '색상';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 0 FROM category c, option_group g WHERE c.category_code = 'STR' AND g.name = '색상';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 0 FROM category c, option_group g WHERE c.category_code = 'GRP' AND g.name = '색상';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 0 FROM category c, option_group g WHERE c.category_code = 'PRT' AND g.name = '문자 사이즈';
INSERT IGNORE INTO category_option_group (category_id, option_group_id, option_type, sort_order) SELECT c.category_id, g.option_group_id, 'SKU', 1 FROM category c, option_group g WHERE c.category_code = 'PRT' AND g.name = '색상';
