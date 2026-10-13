-- 브랜드 시드(ADR-025): 스포츠 용품 브랜드와 영문 3자 코드(ADR-023).
-- 같은 코드나 이름이 이미 있으면 건너뛰므로(INSERT IGNORE) 기존 브랜드는 바뀌지 않는다.

INSERT IGNORE INTO brand (brand_code, name) VALUES ('YNX', '요넥스');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('VCT', '빅터');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('LNG', '리닝');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('MZN', '미즈노');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('BBL', '바볼랏');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('WLS', '윌슨');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('HED', '헤드');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('NIK', '나이키');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('ADS', '아디다스');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('ASC', '아식스');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('KWS', '가와사키');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('PMA', '푸마');
