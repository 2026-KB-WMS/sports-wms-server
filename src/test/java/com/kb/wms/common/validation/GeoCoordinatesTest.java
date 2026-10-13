package com.kb.wms.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.wms.inbound.adapter.in.web.dto.request.SupplierRegisterRequest;
import com.kb.wms.inbound.adapter.in.web.dto.request.SupplierUpdateRequest;
import com.kb.wms.store.adapter.in.web.dto.request.StoreRegisterRequest;
import com.kb.wms.store.adapter.in.web.dto.request.StoreUpdateRequest;
import com.kb.wms.warehouse.adapter.in.web.dto.request.WarehouseRegisterRequest;
import com.kb.wms.warehouse.adapter.in.web.dto.request.WarehouseUpdateRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class GeoCoordinatesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static BigDecimal d(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @Test
    @DisplayName("소수 7자리를 넘는 좌표는 반올림한다")
    void scale() {
        assertThat(GeoCoordinates.scale(d("37.123456789"))).isEqualByComparingTo("37.1234568");
        assertThat(GeoCoordinates.scale(null)).isNull();
    }

    @ParameterizedTest(name = "위도={0}, 경도={1} → 유효={2}")
    @CsvSource(nullValues = "null", value = {
            "37.5665, 126.9780, true",
            "null, null, true",
            "-90, -180, true",
            "90, 180, true",
            "37.5665, null, false",
            "null, 126.9780, false",
            "90.0000001, 126.9780, false",
            "-90.0000001, 126.9780, false",
            "37.5665, 180.0000001, false",
            "37.5665, -180.0000001, false"})
    @DisplayName("창고·지점·공급처 등록·수정 요청이 같은 좌표 규칙을 쓴다")
    void requestsShareCoordinateRules(String lat, String lng, boolean valid) throws Exception {
        BigDecimal latitude = d(lat);
        BigDecimal longitude = d(lng);

        assertThat(validator.validate(new WarehouseRegisterRequest("창고", "서울", null, BigDecimal.TEN,
                latitude, longitude)).isEmpty()).isEqualTo(valid);
        assertThat(validator.validate(new WarehouseUpdateRequest(null, null, null, null, null, null,
                latitude, longitude)).isEmpty()).isEqualTo(valid);
        assertThat(validator.validate(new StoreRegisterRequest("지점", "서울", null, "02-123-4567",
                latitude, longitude)).isEmpty()).isEqualTo(valid);
        assertThat(validator.validate(new StoreUpdateRequest(null, null, null, null, null, null,
                latitude, longitude)).isEmpty()).isEqualTo(valid);
        assertThat(validator.validate(new SupplierRegisterRequest("SUP-1", "공급처", "담당", "02-123-4567", null, null,
                latitude, longitude)).isEmpty()).isEqualTo(valid);

        String json = "{" + (lat == null ? "" : "\"latitude\":" + lat) + (lat != null && lng != null ? "," : "")
                + (lng == null ? "" : "\"longitude\":" + lng) + "}";
        SupplierUpdateRequest update = objectMapper.readValue(json, SupplierUpdateRequest.class);
        assertThat(validator.validate(update).isEmpty()).isEqualTo(valid);
    }
}
