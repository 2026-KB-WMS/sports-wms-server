-- 상품 코드 자동 발급용 채번 테이블(ADR-026).
-- 코드 형식은 {브랜드코드}-{카테고리코드}-{일련번호 4자리}이며, 일련번호는 브랜드+카테고리 조합별로 올린다.
-- 브랜드·카테고리를 바꿔 코드가 재발급되어도 번호가 되돌아가지 않도록 마지막 발급 번호를 별도로 보관한다.
-- 행은 조합의 첫 상품을 등록할 때 만들며, 같은 브랜드의 등록은 브랜드 행 잠금으로 직렬화된다.

CREATE TABLE product_code_sequence (
    brand_id     BIGINT NOT NULL,
    category_id  BIGINT NOT NULL,
    last_no      BIGINT NOT NULL,
    CONSTRAINT pk_product_code_sequence PRIMARY KEY (brand_id, category_id)
);
