-- 상품 도메인 테이블: Brand, Category, Product, ProductSku, OptionGroup, OptionValue, SkuOptionValue
-- 참고: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a

CREATE TABLE brand (
    brand_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(500)  NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_brand_name UNIQUE (name)
);

CREATE TABLE category (
    category_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_category_id  BIGINT        NULL,
    category_code       VARCHAR(50)   NOT NULL,
    name                VARCHAR(100)  NOT NULL,
    depth               TINYINT       NOT NULL DEFAULT 1,
    sort_order          INT           NOT NULL DEFAULT 0,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_category_code UNIQUE (category_code),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_category_id) REFERENCES category (category_id)
);

CREATE INDEX idx_category_parent_category_id ON category (parent_category_id);

CREATE TABLE product (
    product_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_id     BIGINT        NOT NULL,
    category_id  BIGINT        NOT NULL,
    product_code VARCHAR(50)   NOT NULL,
    name         VARCHAR(200)  NOT NULL,
    description  TEXT          NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_code UNIQUE (product_code),
    CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES brand (brand_id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (category_id)
);

CREATE INDEX idx_product_brand_id ON product (brand_id);
CREATE INDEX idx_product_category_id ON product (category_id);

CREATE TABLE product_sku (
    sku_id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id              BIGINT         NOT NULL,
    sku_code                VARCHAR(50)    NOT NULL,
    barcode                 VARCHAR(100)   NULL,
    name                    VARCHAR(200)   NOT NULL,
    weight                  DECIMAL(12,3)  NULL,
    current_purchase_price  DECIMAL(18,2)  NULL,
    current_supply_price    DECIMAL(18,2)  NULL,
    unit                    VARCHAR(20)    NOT NULL DEFAULT 'EA',
    safety_stock_quantity   DECIMAL(14,3)  NOT NULL DEFAULT 0,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_sku_code UNIQUE (sku_code),
    CONSTRAINT uk_product_sku_barcode UNIQUE (barcode),
    CONSTRAINT fk_product_sku_product FOREIGN KEY (product_id) REFERENCES product (product_id),
    CONSTRAINT ck_product_sku_purchase_price CHECK (current_purchase_price IS NULL OR current_purchase_price >= 0),
    CONSTRAINT ck_product_sku_supply_price CHECK (current_supply_price IS NULL OR current_supply_price >= 0)
);

CREATE INDEX idx_product_sku_product_id ON product_sku (product_id);

CREATE TABLE option_group (
    option_group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_option_group_name UNIQUE (name)
);

CREATE TABLE option_value (
    option_value_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    option_group_id  BIGINT       NOT NULL,
    value            VARCHAR(100) NOT NULL,
    sort_order       INT          NOT NULL DEFAULT 0,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_option_value_group_value UNIQUE (option_group_id, value),
    CONSTRAINT fk_option_value_group FOREIGN KEY (option_group_id) REFERENCES option_group (option_group_id)
);

CREATE TABLE sku_option_value (
    sku_id           BIGINT   NOT NULL,
    option_value_id  BIGINT   NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sku_id, option_value_id),
    CONSTRAINT fk_sku_option_value_sku FOREIGN KEY (sku_id) REFERENCES product_sku (sku_id),
    CONSTRAINT fk_sku_option_value_option_value FOREIGN KEY (option_value_id) REFERENCES option_value (option_value_id)
);

CREATE INDEX idx_sku_option_value_option_value_id ON sku_option_value (option_value_id);
