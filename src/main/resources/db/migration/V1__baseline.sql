-- WMS 스키마 baseline (이슈 #283).
-- 기존 V1~V28 마이그레이션을 최종 스키마와 시드 데이터로 통합한 파일이다. 이후 변경은 V2부터 추가한다.
--
-- 구성
--   1) 테이블 (FK 제약은 제외)
--   2) 외래 키 (참조 순서와 무관하게 한 번에 추가)
--   3) 시드 데이터 (위치 코드 시퀀스, 카테고리·옵션 마스터, 브랜드)
--
-- 규칙
--   - 엔티티는 @ManyToOne 없이 ID로만 참조하지만 DB에는 FK를 건다(ADR-005).
--   - 수량 컬럼은 BIGINT(DECIMAL 금지).
--   - 제약·인덱스 이름은 ConstraintErrorCodeProvider가 의존하므로 바꾸지 않는다.

-- ============================================================
-- 1. 테이블
-- ============================================================

CREATE TABLE `brand` (
  `brand_id` bigint NOT NULL AUTO_INCREMENT,
  `brand_code` varchar(3) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`brand_id`),
  UNIQUE KEY `uk_brand_name` (`name`),
  UNIQUE KEY `uk_brand_code` (`brand_code`)
);

CREATE TABLE `category` (
  `category_id` bigint NOT NULL AUTO_INCREMENT,
  `parent_category_id` bigint DEFAULT NULL,
  `category_code` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `depth` tinyint NOT NULL DEFAULT '1',
  `sort_order` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_category_parent_category_id` (`parent_category_id`)
);

CREATE TABLE `product` (
  `product_id` bigint NOT NULL AUTO_INCREMENT,
  `brand_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `product_code` varchar(50) NOT NULL,
  `name` varchar(200) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `uk_product_code` (`product_code`),
  KEY `idx_product_brand_id` (`brand_id`),
  KEY `idx_product_category_id` (`category_id`)
);

CREATE TABLE `product_sku` (
  `sku_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `sku_code` varchar(50) NOT NULL,
  `barcode` varchar(100) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `weight` decimal(12,3) DEFAULT NULL,
  `current_purchase_price` decimal(18,2) NOT NULL,
  `current_supply_price` decimal(18,2) NOT NULL,
  `unit` varchar(20) NOT NULL DEFAULT 'EA',
  `safety_stock_quantity` bigint NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sku_id`),
  UNIQUE KEY `uk_product_sku_code` (`sku_code`),
  UNIQUE KEY `uk_product_sku_barcode` (`barcode`),
  KEY `idx_product_sku_product_id` (`product_id`),
  CONSTRAINT `ck_product_sku_purchase_price` CHECK (((`current_purchase_price` is null) or (`current_purchase_price` >= 0))),
  CONSTRAINT `ck_product_sku_supply_price` CHECK (((`current_supply_price` is null) or (`current_supply_price` >= 0)))
);

CREATE TABLE `option_group` (
  `option_group_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`option_group_id`),
  UNIQUE KEY `uk_option_group_name` (`name`)
);

CREATE TABLE `option_value` (
  `option_value_id` bigint NOT NULL AUTO_INCREMENT,
  `option_group_id` bigint NOT NULL,
  `option_code` varchar(10) DEFAULT NULL,
  `value` varchar(100) NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`option_value_id`),
  UNIQUE KEY `uk_option_value_group_value` (`option_group_id`,`value`),
  UNIQUE KEY `uk_option_value_group_code` (`option_group_id`,`option_code`)
);

CREATE TABLE `sku_option_value` (
  `sku_id` bigint NOT NULL,
  `option_value_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`sku_id`,`option_value_id`),
  KEY `idx_sku_option_value_option_value_id` (`option_value_id`)
);

CREATE TABLE `warehouse` (
  `warehouse_id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_code` varchar(30) NOT NULL,
  `name` varchar(100) NOT NULL,
  `address` varchar(500) NOT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `contact_number` varchar(30) DEFAULT NULL,
  `total_capacity` decimal(14,3) NOT NULL DEFAULT '0.000',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`warehouse_id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_code`)
);

CREATE TABLE `warehouse_section` (
  `section_id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  `parent_section_id` bigint DEFAULT NULL,
  `section_code` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `section_type` varchar(20) NOT NULL,
  `capacity` decimal(14,3) NOT NULL DEFAULT '0.000',
  `current_capacity` decimal(14,3) NOT NULL DEFAULT '0.000',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`section_id`),
  UNIQUE KEY `uk_warehouse_section_code` (`warehouse_id`,`section_code`),
  KEY `idx_warehouse_section_warehouse_id` (`warehouse_id`),
  KEY `idx_warehouse_section_parent_section_id` (`parent_section_id`)
);

CREATE TABLE `warehouse_member` (
  `warehouse_member_id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `member_role` varchar(30) NOT NULL,
  `assigned_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`warehouse_member_id`),
  UNIQUE KEY `uk_warehouse_member_warehouse_user` (`warehouse_id`,`user_id`),
  KEY `idx_warehouse_member_user_id` (`user_id`)
);

CREATE TABLE `lot` (
  `lot_id` bigint NOT NULL AUTO_INCREMENT,
  `sku_id` bigint NOT NULL,
  `supplier_id` bigint NOT NULL,
  `lot_number` varchar(100) NOT NULL,
  `manufactured_date` date DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'AVAILABLE',
  `unit_cost` decimal(18,2) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`lot_id`),
  UNIQUE KEY `uk_lot_sku_supplier_number` (`sku_id`,`supplier_id`,`lot_number`),
  KEY `idx_lot_supplier_id` (`supplier_id`),
  CONSTRAINT `ck_lot_unit_cost` CHECK ((`unit_cost` >= 0))
);

CREATE TABLE `inventory_lot` (
  `inventory_lot_id` bigint NOT NULL AUTO_INCREMENT,
  `section_id` bigint NOT NULL,
  `lot_id` bigint NOT NULL,
  `on_hand_quantity` bigint NOT NULL DEFAULT '0',
  `allocated_quantity` bigint NOT NULL DEFAULT '0',
  `quality_status` varchar(20) NOT NULL DEFAULT 'AVAILABLE',
  `last_counted_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`inventory_lot_id`),
  UNIQUE KEY `uk_inventory_lot_section_lot` (`section_id`,`lot_id`),
  KEY `idx_inventory_lot_lot_id` (`lot_id`),
  CONSTRAINT `ck_inventory_lot_quantity` CHECK (((`allocated_quantity` >= 0) and (`allocated_quantity` <= `on_hand_quantity`)))
);

CREATE TABLE `inventory_transaction` (
  `transaction_id` bigint NOT NULL AUTO_INCREMENT,
  `inventory_lot_id` bigint NOT NULL,
  `transaction_type` varchar(30) NOT NULL,
  `quantity_delta` bigint NOT NULL,
  `before_quantity` bigint NOT NULL,
  `after_quantity` bigint NOT NULL,
  `reference_type` varchar(30) NOT NULL,
  `reference_id` bigint DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`transaction_id`),
  KEY `idx_inventory_transaction_lot_created` (`inventory_lot_id`,`created_at`),
  KEY `idx_inventory_transaction_created_at` (`created_at`),
  KEY `idx_inventory_transaction_reference` (`reference_type`,`reference_id`),
  KEY `idx_inventory_transaction_created_by` (`created_by`)
);

CREATE TABLE `store` (
  `store_id` bigint NOT NULL AUTO_INCREMENT,
  `store_code` varchar(30) NOT NULL,
  `name` varchar(100) NOT NULL,
  `address` varchar(500) NOT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `contact_name` varchar(100) DEFAULT NULL,
  `contact_number` varchar(30) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`store_id`),
  UNIQUE KEY `uk_store_code` (`store_code`)
);

CREATE TABLE `store_member` (
  `store_member_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `member_role` varchar(30) NOT NULL,
  `assigned_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`store_member_id`),
  UNIQUE KEY `uk_store_member_store_user` (`store_id`,`user_id`),
  KEY `idx_store_member_user_id` (`user_id`)
);

CREATE TABLE `supplier` (
  `supplier_id` bigint NOT NULL AUTO_INCREMENT,
  `supplier_code` varchar(30) NOT NULL,
  `name` varchar(200) NOT NULL,
  `manager_name` varchar(100) NOT NULL,
  `contact_number` varchar(30) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `address` varchar(500) DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`)
);

CREATE TABLE `purchase_order` (
  `purchase_order_id` bigint NOT NULL AUTO_INCREMENT,
  `purchase_order_no` varchar(50) NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'REQUESTED',
  `expected_at` datetime DEFAULT NULL,
  `note` varchar(1000) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`purchase_order_id`),
  UNIQUE KEY `uk_purchase_order_no` (`purchase_order_no`),
  KEY `idx_purchase_order_warehouse_status` (`warehouse_id`,`status`),
  KEY `idx_purchase_order_created_by` (`created_by`)
);

CREATE TABLE `purchase_order_line` (
  `purchase_order_line_id` bigint NOT NULL AUTO_INCREMENT,
  `purchase_order_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `supplier_id` bigint DEFAULT NULL,
  `supplier_order_id` bigint DEFAULT NULL,
  `expected_quantity` bigint NOT NULL,
  `received_quantity` bigint NOT NULL DEFAULT '0',
  `line_amount` decimal(18,2) NOT NULL,
  `ordered_unit_price` decimal(18,2) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'REQUESTED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`purchase_order_line_id`),
  UNIQUE KEY `uk_purchase_order_line_order_sku` (`purchase_order_id`,`sku_id`),
  KEY `idx_purchase_order_line_sku_id` (`sku_id`),
  KEY `idx_purchase_order_line_supplier_id` (`supplier_id`),
  KEY `idx_purchase_order_line_supplier_order_id` (`supplier_order_id`),
  CONSTRAINT `ck_purchase_order_line_amount` CHECK ((`line_amount` >= 0)),
  CONSTRAINT `ck_purchase_order_line_expected_quantity` CHECK ((`expected_quantity` > 0)),
  CONSTRAINT `ck_purchase_order_line_received_quantity` CHECK ((`received_quantity` >= 0)),
  CONSTRAINT `ck_purchase_order_line_unit_price` CHECK ((`ordered_unit_price` >= 0))
);

CREATE TABLE `inbound` (
  `inbound_id` bigint NOT NULL AUTO_INCREMENT,
  `inbound_no` varchar(50) NOT NULL,
  `purchase_order_id` bigint NOT NULL,
  `supplier_order_id` bigint DEFAULT NULL,
  `warehouse_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ARRIVED',
  `arrived_at` datetime NOT NULL,
  `received_at` datetime DEFAULT NULL,
  `received_by` bigint DEFAULT NULL,
  `note` varchar(1000) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`inbound_id`),
  UNIQUE KEY `uk_inbound_no` (`inbound_no`),
  KEY `idx_inbound_purchase_order_status` (`purchase_order_id`,`status`),
  KEY `idx_inbound_warehouse_status` (`warehouse_id`,`status`),
  KEY `idx_inbound_received_by` (`received_by`),
  KEY `idx_inbound_supplier_order_id` (`supplier_order_id`)
);

CREATE TABLE `inbound_line` (
  `inbound_line_id` bigint NOT NULL AUTO_INCREMENT,
  `inbound_id` bigint NOT NULL,
  `purchase_order_line_id` bigint NOT NULL,
  `lot_id` bigint NOT NULL,
  `accepted_section_id` bigint DEFAULT NULL,
  `defect_section_id` bigint DEFAULT NULL,
  `received_quantity` bigint NOT NULL,
  `accepted_quantity` bigint NOT NULL DEFAULT '0',
  `defective_quantity` bigint NOT NULL DEFAULT '0',
  `received_unit_price` decimal(18,2) NOT NULL,
  `line_amount` decimal(18,2) NOT NULL,
  `price_change_reason` varchar(500) DEFAULT NULL,
  `inspection_note` varchar(1000) DEFAULT NULL,
  `received_at` datetime NOT NULL,
  `received_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`inbound_line_id`),
  UNIQUE KEY `uk_inbound_line_inbound_po_line_lot` (`inbound_id`,`purchase_order_line_id`,`lot_id`),
  KEY `idx_inbound_line_purchase_order_line_id` (`purchase_order_line_id`),
  KEY `idx_inbound_line_lot_id` (`lot_id`),
  KEY `idx_inbound_line_accepted_section_id` (`accepted_section_id`),
  KEY `idx_inbound_line_defect_section_id` (`defect_section_id`),
  KEY `idx_inbound_line_received_by` (`received_by`),
  CONSTRAINT `ck_inbound_line_accepted_quantity` CHECK ((`accepted_quantity` >= 0)),
  CONSTRAINT `ck_inbound_line_amount` CHECK ((`line_amount` >= 0)),
  CONSTRAINT `ck_inbound_line_defective_quantity` CHECK ((`defective_quantity` >= 0)),
  CONSTRAINT `ck_inbound_line_quantity_sum` CHECK (((`accepted_quantity` + `defective_quantity`) = `received_quantity`)),
  CONSTRAINT `ck_inbound_line_received_quantity` CHECK ((`received_quantity` > 0)),
  CONSTRAINT `ck_inbound_line_unit_price` CHECK ((`received_unit_price` >= 0))
);

CREATE TABLE `status_history` (
  `status_history_id` bigint NOT NULL AUTO_INCREMENT,
  `entity_type` varchar(30) NOT NULL,
  `entity_id` bigint NOT NULL,
  `from_status` varchar(20) DEFAULT NULL,
  `to_status` varchar(20) NOT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `changed_by` bigint NOT NULL,
  `changed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`status_history_id`),
  KEY `idx_status_history_entity` (`entity_type`,`entity_id`,`changed_at`),
  KEY `idx_status_history_changed_by` (`changed_by`)
);

CREATE TABLE `store_order` (
  `store_order_id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(50) NOT NULL,
  `store_id` bigint NOT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'REQUESTED',
  `requested_at` datetime NOT NULL,
  `requested_delivery_at` datetime DEFAULT NULL,
  `note` varchar(1000) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`store_order_id`),
  UNIQUE KEY `uk_store_order_no` (`order_no`),
  KEY `idx_store_order_store_status` (`store_id`,`status`),
  KEY `idx_store_order_warehouse_status` (`warehouse_id`,`status`),
  KEY `idx_store_order_status_requested_at` (`status`,`requested_at`),
  KEY `idx_store_order_created_by` (`created_by`)
);

CREATE TABLE `store_order_line` (
  `store_order_line_id` bigint NOT NULL AUTO_INCREMENT,
  `store_order_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `requested_quantity` bigint NOT NULL,
  `allocated_quantity` bigint NOT NULL DEFAULT '0',
  `shipped_quantity` bigint NOT NULL DEFAULT '0',
  `requested_unit_supply_price` decimal(18,2) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'REQUESTED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`store_order_line_id`),
  UNIQUE KEY `uk_store_order_line_order_sku` (`store_order_id`,`sku_id`),
  KEY `idx_store_order_line_sku_id` (`sku_id`),
  CONSTRAINT `ck_store_order_line_allocated_quantity` CHECK ((`allocated_quantity` >= 0)),
  CONSTRAINT `ck_store_order_line_requested_quantity` CHECK ((`requested_quantity` > 0)),
  CONSTRAINT `ck_store_order_line_shipped_quantity` CHECK ((`shipped_quantity` >= 0)),
  CONSTRAINT `ck_store_order_line_unit_supply_price` CHECK ((`requested_unit_supply_price` >= 0))
);

CREATE TABLE `stock_allocation` (
  `allocation_id` bigint NOT NULL AUTO_INCREMENT,
  `store_order_line_id` bigint NOT NULL,
  `inventory_lot_id` bigint NOT NULL,
  `allocated_quantity` bigint NOT NULL,
  `picked_quantity` bigint NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'ALLOCATED',
  `allocated_at` datetime NOT NULL,
  `released_at` datetime DEFAULT NULL,
  `allocated_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`allocation_id`),
  KEY `idx_stock_allocation_line_status` (`store_order_line_id`,`status`),
  KEY `idx_stock_allocation_lot_status` (`inventory_lot_id`,`status`),
  KEY `idx_stock_allocation_allocated_by` (`allocated_by`),
  CONSTRAINT `ck_stock_allocation_allocated_quantity` CHECK ((`allocated_quantity` > 0)),
  CONSTRAINT `ck_stock_allocation_picked_quantity` CHECK (((`picked_quantity` >= 0) and (`picked_quantity` <= `allocated_quantity`))),
  CONSTRAINT `ck_stock_allocation_status` CHECK ((`status` in (_utf8mb4'ALLOCATED',_utf8mb4'PICKED',_utf8mb4'RELEASED')))
);

CREATE TABLE `outbound` (
  `outbound_id` bigint NOT NULL AUTO_INCREMENT,
  `outbound_no` varchar(30) NOT NULL,
  `store_order_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'READY',
  `shipped_at` datetime DEFAULT NULL,
  `shipped_by` bigint DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`outbound_id`),
  UNIQUE KEY `uk_outbound_no` (`outbound_no`),
  KEY `idx_outbound_order_status` (`store_order_id`,`status`),
  KEY `idx_outbound_status_created` (`status`,`created_at`),
  KEY `idx_outbound_shipped_by` (`shipped_by`),
  CONSTRAINT `ck_outbound_status` CHECK ((`status` in (_utf8mb4'READY',_utf8mb4'PICKING',_utf8mb4'PICKED',_utf8mb4'SHIPPED',_utf8mb4'DELIVERED',_utf8mb4'CANCELED')))
);

CREATE TABLE `outbound_line` (
  `outbound_line_id` bigint NOT NULL AUTO_INCREMENT,
  `outbound_id` bigint NOT NULL,
  `allocation_id` bigint NOT NULL,
  `shipped_quantity` bigint NOT NULL DEFAULT '0',
  `confirmed_unit_supply_price` decimal(18,2) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`outbound_line_id`),
  UNIQUE KEY `uk_outbound_line_outbound_allocation` (`outbound_id`,`allocation_id`),
  KEY `idx_outbound_line_allocation` (`allocation_id`),
  CONSTRAINT `ck_outbound_line_shipped_quantity` CHECK ((`shipped_quantity` >= 0)),
  CONSTRAINT `ck_outbound_line_unit_supply_price` CHECK (((`confirmed_unit_supply_price` is null) or (`confirmed_unit_supply_price` >= 0)))
);

CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `login_id` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `name` varchar(100) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `role` varchar(30) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_users_login_id` (`login_id`),
  UNIQUE KEY `uk_users_email` (`email`),
  KEY `idx_users_role_status` (`role`,`status`)
);

CREATE TABLE `sku_price_history` (
  `sku_price_history_id` bigint NOT NULL AUTO_INCREMENT,
  `sku_id` bigint NOT NULL,
  `previous_purchase_price` decimal(18,2) NOT NULL,
  `new_purchase_price` decimal(18,2) NOT NULL,
  `previous_supply_price` decimal(18,2) NOT NULL,
  `new_supply_price` decimal(18,2) NOT NULL,
  `changed_by` bigint NOT NULL,
  `changed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`sku_price_history_id`),
  KEY `idx_sku_price_history_sku` (`sku_id`,`changed_at`),
  KEY `idx_sku_price_history_changed_by` (`changed_by`)
);

CREATE TABLE `location_code_sequence` (
  `sequence_key` varchar(20) NOT NULL,
  `last_no` bigint NOT NULL,
  PRIMARY KEY (`sequence_key`)
);

CREATE TABLE `category_option_group` (
  `category_option_group_id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `option_group_id` bigint NOT NULL,
  `option_type` varchar(10) NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_option_group_id`),
  UNIQUE KEY `uk_category_option_group` (`category_id`,`option_group_id`),
  KEY `fk_category_option_group_option_group` (`option_group_id`)
);

CREATE TABLE `product_spec` (
  `product_spec_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `option_value_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_spec_id`),
  UNIQUE KEY `uk_product_spec` (`product_id`,`option_value_id`),
  KEY `idx_product_spec_option_value_id` (`option_value_id`)
);

CREATE TABLE `product_code_sequence` (
  `brand_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `last_no` bigint NOT NULL,
  PRIMARY KEY (`brand_id`,`category_id`)
);

CREATE TABLE `sku_supplier` (
  `sku_supplier_id` bigint NOT NULL AUTO_INCREMENT,
  `sku_id` bigint NOT NULL,
  `supplier_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`sku_supplier_id`),
  UNIQUE KEY `uk_sku_supplier_sku_supplier` (`sku_id`,`supplier_id`),
  KEY `idx_sku_supplier_supplier_id` (`supplier_id`)
);

CREATE TABLE `supplier_order` (
  `supplier_order_id` bigint NOT NULL AUTO_INCREMENT,
  `supplier_order_no` varchar(50) NOT NULL,
  `purchase_order_id` bigint NOT NULL,
  `supplier_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ISSUED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_order_id`),
  UNIQUE KEY `uk_supplier_order_no` (`supplier_order_no`),
  UNIQUE KEY `uk_supplier_order_po_supplier` (`purchase_order_id`,`supplier_id`),
  KEY `idx_supplier_order_supplier_status` (`supplier_id`,`status`)
);


-- ============================================================
-- 2. 외래 키
-- ============================================================

ALTER TABLE `category`
    ADD CONSTRAINT `fk_category_parent` FOREIGN KEY (`parent_category_id`) REFERENCES `category` (`category_id`);

ALTER TABLE `product`
    ADD CONSTRAINT `fk_product_brand` FOREIGN KEY (`brand_id`) REFERENCES `brand` (`brand_id`);

ALTER TABLE `product`
    ADD CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`);

ALTER TABLE `product_sku`
    ADD CONSTRAINT `fk_product_sku_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`);

ALTER TABLE `option_value`
    ADD CONSTRAINT `fk_option_value_group` FOREIGN KEY (`option_group_id`) REFERENCES `option_group` (`option_group_id`);

ALTER TABLE `sku_option_value`
    ADD CONSTRAINT `fk_sku_option_value_option_value` FOREIGN KEY (`option_value_id`) REFERENCES `option_value` (`option_value_id`);

ALTER TABLE `sku_option_value`
    ADD CONSTRAINT `fk_sku_option_value_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_sku` (`sku_id`);

ALTER TABLE `warehouse_section`
    ADD CONSTRAINT `fk_warehouse_section_parent` FOREIGN KEY (`parent_section_id`) REFERENCES `warehouse_section` (`section_id`);

ALTER TABLE `warehouse_section`
    ADD CONSTRAINT `fk_warehouse_section_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE `warehouse_member`
    ADD CONSTRAINT `fk_warehouse_member_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `warehouse_member`
    ADD CONSTRAINT `fk_warehouse_member_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE `lot`
    ADD CONSTRAINT `fk_lot_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_sku` (`sku_id`);

ALTER TABLE `lot`
    ADD CONSTRAINT `fk_lot_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`supplier_id`);

ALTER TABLE `inventory_lot`
    ADD CONSTRAINT `fk_inventory_lot_lot` FOREIGN KEY (`lot_id`) REFERENCES `lot` (`lot_id`);

ALTER TABLE `inventory_lot`
    ADD CONSTRAINT `fk_inventory_lot_section` FOREIGN KEY (`section_id`) REFERENCES `warehouse_section` (`section_id`);

ALTER TABLE `inventory_transaction`
    ADD CONSTRAINT `fk_inventory_transaction_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `inventory_transaction`
    ADD CONSTRAINT `fk_inventory_transaction_inventory_lot` FOREIGN KEY (`inventory_lot_id`) REFERENCES `inventory_lot` (`inventory_lot_id`);

ALTER TABLE `store_member`
    ADD CONSTRAINT `fk_store_member_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`);

ALTER TABLE `store_member`
    ADD CONSTRAINT `fk_store_member_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `purchase_order`
    ADD CONSTRAINT `fk_purchase_order_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `purchase_order`
    ADD CONSTRAINT `fk_purchase_order_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE `purchase_order_line`
    ADD CONSTRAINT `fk_purchase_order_line_order` FOREIGN KEY (`purchase_order_id`) REFERENCES `purchase_order` (`purchase_order_id`);

ALTER TABLE `purchase_order_line`
    ADD CONSTRAINT `fk_purchase_order_line_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_sku` (`sku_id`);

ALTER TABLE `purchase_order_line`
    ADD CONSTRAINT `fk_purchase_order_line_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`supplier_id`);

ALTER TABLE `purchase_order_line`
    ADD CONSTRAINT `fk_purchase_order_line_supplier_order` FOREIGN KEY (`supplier_order_id`) REFERENCES `supplier_order` (`supplier_order_id`);

ALTER TABLE `inbound`
    ADD CONSTRAINT `fk_inbound_purchase_order` FOREIGN KEY (`purchase_order_id`) REFERENCES `purchase_order` (`purchase_order_id`);

ALTER TABLE `inbound`
    ADD CONSTRAINT `fk_inbound_received_by` FOREIGN KEY (`received_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `inbound`
    ADD CONSTRAINT `fk_inbound_supplier_order` FOREIGN KEY (`supplier_order_id`) REFERENCES `supplier_order` (`supplier_order_id`);

ALTER TABLE `inbound`
    ADD CONSTRAINT `fk_inbound_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE `inbound_line`
    ADD CONSTRAINT `fk_inbound_line_accepted_section` FOREIGN KEY (`accepted_section_id`) REFERENCES `warehouse_section` (`section_id`);

ALTER TABLE `inbound_line`
    ADD CONSTRAINT `fk_inbound_line_defect_section` FOREIGN KEY (`defect_section_id`) REFERENCES `warehouse_section` (`section_id`);

ALTER TABLE `inbound_line`
    ADD CONSTRAINT `fk_inbound_line_inbound` FOREIGN KEY (`inbound_id`) REFERENCES `inbound` (`inbound_id`);

ALTER TABLE `inbound_line`
    ADD CONSTRAINT `fk_inbound_line_lot` FOREIGN KEY (`lot_id`) REFERENCES `lot` (`lot_id`);

ALTER TABLE `inbound_line`
    ADD CONSTRAINT `fk_inbound_line_purchase_order_line` FOREIGN KEY (`purchase_order_line_id`) REFERENCES `purchase_order_line` (`purchase_order_line_id`);

ALTER TABLE `inbound_line`
    ADD CONSTRAINT `fk_inbound_line_received_by` FOREIGN KEY (`received_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `status_history`
    ADD CONSTRAINT `fk_status_history_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `store_order`
    ADD CONSTRAINT `fk_store_order_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `store_order`
    ADD CONSTRAINT `fk_store_order_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`);

ALTER TABLE `store_order`
    ADD CONSTRAINT `fk_store_order_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE `store_order_line`
    ADD CONSTRAINT `fk_store_order_line_order` FOREIGN KEY (`store_order_id`) REFERENCES `store_order` (`store_order_id`);

ALTER TABLE `store_order_line`
    ADD CONSTRAINT `fk_store_order_line_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_sku` (`sku_id`);

ALTER TABLE `stock_allocation`
    ADD CONSTRAINT `fk_stock_allocation_allocated_by` FOREIGN KEY (`allocated_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `stock_allocation`
    ADD CONSTRAINT `fk_stock_allocation_inventory_lot` FOREIGN KEY (`inventory_lot_id`) REFERENCES `inventory_lot` (`inventory_lot_id`);

ALTER TABLE `stock_allocation`
    ADD CONSTRAINT `fk_stock_allocation_store_order_line` FOREIGN KEY (`store_order_line_id`) REFERENCES `store_order_line` (`store_order_line_id`);

ALTER TABLE `outbound`
    ADD CONSTRAINT `fk_outbound_shipped_by` FOREIGN KEY (`shipped_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `outbound`
    ADD CONSTRAINT `fk_outbound_store_order` FOREIGN KEY (`store_order_id`) REFERENCES `store_order` (`store_order_id`);

ALTER TABLE `outbound_line`
    ADD CONSTRAINT `fk_outbound_line_allocation` FOREIGN KEY (`allocation_id`) REFERENCES `stock_allocation` (`allocation_id`);

ALTER TABLE `outbound_line`
    ADD CONSTRAINT `fk_outbound_line_outbound` FOREIGN KEY (`outbound_id`) REFERENCES `outbound` (`outbound_id`);

ALTER TABLE `sku_price_history`
    ADD CONSTRAINT `fk_sku_price_history_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `sku_price_history`
    ADD CONSTRAINT `fk_sku_price_history_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_sku` (`sku_id`);

ALTER TABLE `category_option_group`
    ADD CONSTRAINT `fk_category_option_group_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`);

ALTER TABLE `category_option_group`
    ADD CONSTRAINT `fk_category_option_group_option_group` FOREIGN KEY (`option_group_id`) REFERENCES `option_group` (`option_group_id`);

ALTER TABLE `product_spec`
    ADD CONSTRAINT `fk_product_spec_option_value` FOREIGN KEY (`option_value_id`) REFERENCES `option_value` (`option_value_id`);

ALTER TABLE `product_spec`
    ADD CONSTRAINT `fk_product_spec_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`);

ALTER TABLE `sku_supplier`
    ADD CONSTRAINT `fk_sku_supplier_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_sku` (`sku_id`);

ALTER TABLE `sku_supplier`
    ADD CONSTRAINT `fk_sku_supplier_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`supplier_id`);

ALTER TABLE `supplier_order`
    ADD CONSTRAINT `fk_supplier_order_purchase_order` FOREIGN KEY (`purchase_order_id`) REFERENCES `purchase_order` (`purchase_order_id`);

ALTER TABLE `supplier_order`
    ADD CONSTRAINT `fk_supplier_order_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`supplier_id`);


-- ============================================================
-- 3. 시드 데이터
-- ============================================================

-- 위치 코드 시퀀스 초기값
INSERT INTO location_code_sequence (sequence_key, last_no)
SELECT CONCAT(t.prefix, '-', r.region), 0
FROM (SELECT 'WH' AS prefix UNION ALL SELECT 'ST') t
CROSS JOIN (
    SELECT 'SU' AS region UNION ALL SELECT 'BS' UNION ALL SELECT 'DG' UNION ALL SELECT 'IC'
    UNION ALL SELECT 'GJ' UNION ALL SELECT 'DJ' UNION ALL SELECT 'US' UNION ALL SELECT 'SJ'
    UNION ALL SELECT 'GG' UNION ALL SELECT 'GW' UNION ALL SELECT 'CB' UNION ALL SELECT 'CN'
    UNION ALL SELECT 'JB' UNION ALL SELECT 'JN' UNION ALL SELECT 'GB' UNION ALL SELECT 'GN'
    UNION ALL SELECT 'JJ'
) r;

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

-- 브랜드 시드(ADR-025): 스포츠 용품 브랜드와 영문 3자 코드(ADR-023).
-- 같은 코드나 이름이 이미 있으면 건너뛰므로(INSERT IGNORE) 기존 브랜드는 바뀌지 않는다.

INSERT IGNORE INTO brand (brand_code, name) VALUES ('YNX', '요넥스');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('VCT', '빅터');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('LNG', '리닝');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('TCH', '테크니스트');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('APX', '아펙스');
INSERT IGNORE INTO brand (brand_code, name) VALUES ('TRI', '트라이온');

