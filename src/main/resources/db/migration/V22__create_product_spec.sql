-- 상품에 지정한 스펙 값(ADR-025). 상품당 옵션 그룹 하나에 값 하나는 서비스에서 검증한다.

CREATE TABLE product_spec (
    product_spec_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id       BIGINT   NOT NULL,
    option_value_id  BIGINT   NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_spec UNIQUE (product_id, option_value_id),
    CONSTRAINT fk_product_spec_product FOREIGN KEY (product_id) REFERENCES product (product_id),
    CONSTRAINT fk_product_spec_option_value FOREIGN KEY (option_value_id) REFERENCES option_value (option_value_id)
);

CREATE INDEX idx_product_spec_option_value_id ON product_spec (option_value_id);
