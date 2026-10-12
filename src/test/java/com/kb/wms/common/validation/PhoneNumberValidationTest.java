package com.kb.wms.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.kb.wms.store.adapter.in.web.dto.request.StoreUpdateRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PhoneNumberValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean valid(String contactNumber) {
        return validator.validate(new StoreUpdateRequest(null, null, null, contactNumber, null, null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"010-1234-5678", "010-123-4567", "02-123-4567", "02-1234-5678", "031-123-4567", "031-1234-5678"})
    void 허용_형식은_통과한다(String number) {
        assertThat(valid(number)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"01012345678", "010 1234 5678", "0-123-4567", "0101-1234-5678", "02-12-4567", "02-12345-5678", "02-123-456",
            "02-123-45678", "1588-1234", "abc-defg-hijk", "", " 02-123-4567"})
    void 형식이_틀리면_거절한다(String number) {
        assertThat(valid(number)).isFalse();
    }

    @Test
    void 생략하면_통과한다() {
        assertThat(valid(null)).isTrue();
    }
}
