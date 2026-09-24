package com.kb.wms.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLIntegrityConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import com.kb.wms.common.response.ErrorResponse;
import com.kb.wms.product.exception.ProductConstraintErrorCodes;
import com.kb.wms.warehouse.exception.WarehouseConstraintErrorCodes;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("product", new ProductConstraintErrorCodes());
        beanFactory.registerSingleton("warehouse", new WarehouseConstraintErrorCodes());
        handler = new GlobalExceptionHandler(beanFactory.getBeanProvider(ConstraintErrorCodeProvider.class));
    }

    @Test
    @DisplayName("유니크 제약 위반(MySQL 메시지)은 제약 이름에 대응하는 도메인 오류 코드로 409 응답한다")
    void uniqueViolation_mysqlMessage_mapsToDomainErrorCode() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(violation(
                "Duplicate entry '1-A-01' for key 'warehouse_section.uk_warehouse_section_code'"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().errorCode()).isEqualTo("DUPLICATE_SECTION_CODE");
    }

    @Test
    @DisplayName("유니크 제약 위반(H2 대문자 메시지)도 같은 오류 코드로 매핑하고, 이름이 겹치면 더 긴 제약 이름을 우선한다")
    void uniqueViolation_h2Message_prefersLongestConstraintName() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(violation(
                "Unique index or primary key violation: \"PUBLIC.UK_PRODUCT_SKU_CODE_INDEX_2 ON PUBLIC.PRODUCT_SKU(SKU_CODE)\""));

        assertThat(response.getBody().errorCode()).isEqualTo("DUPLICATE_SKU_CODE");
    }

    @Test
    @DisplayName("대응표에 없는 제약 위반은 409 CONFLICT로 응답한다")
    void unmappedViolation_returnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(violation(
                "Cannot add or update a child row: a foreign key constraint fails"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().errorCode()).isEqualTo("CONFLICT");
    }

    @Test
    @DisplayName("잠금 획득 실패(대기 시간 초과·교착)는 409 CONFLICT로 응답한다")
    void lockFailure_returnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleLockFailure(
                new CannotAcquireLockException("Lock wait timeout exceeded"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().errorCode()).isEqualTo("CONFLICT");
    }

    private static DataIntegrityViolationException violation(String causeMessage) {
        return new DataIntegrityViolationException("could not execute statement",
                new SQLIntegrityConstraintViolationException(causeMessage));
    }
}
