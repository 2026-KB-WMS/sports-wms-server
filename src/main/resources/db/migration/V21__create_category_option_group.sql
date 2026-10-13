-- 카테고리별 옵션 그룹 지정. 용도(SPEC·SKU)는 카테고리마다 정한다(ADR-025).

CREATE TABLE category_option_group (
    category_option_group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id              BIGINT      NOT NULL,
    option_group_id          BIGINT      NOT NULL,
    option_type              VARCHAR(10) NOT NULL,
    sort_order               INT         NOT NULL DEFAULT 0,
    created_at               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_category_option_group UNIQUE (category_id, option_group_id),
    CONSTRAINT fk_category_option_group_category FOREIGN KEY (category_id) REFERENCES category (category_id),
    CONSTRAINT fk_category_option_group_option_group FOREIGN KEY (option_group_id) REFERENCES option_group (option_group_id)
);
