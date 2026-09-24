package com.kb.wms.common.exception;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.kb.wms.common.response.ErrorResponse;

/**
 * 전역 예외 처리기.
 * 모든 예외를 공통 API 규칙 문서의 오류 응답 포맷으로 변환한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String DATA_CONFLICT_MESSAGE = "데이터 제약 조건과 충돌하는 요청입니다. 현재 상태를 다시 확인해주세요.";
    private static final String LOCK_CONFLICT_MESSAGE = "다른 요청이 같은 데이터를 처리하고 있습니다. 잠시 후 다시 시도해주세요.";

    private final ObjectProvider<ConstraintErrorCodeProvider> constraintErrorCodeProviders;

    public GlobalExceptionHandler(ObjectProvider<ConstraintErrorCodeProvider> constraintErrorCodeProviders) {
        this.constraintErrorCodeProviders = constraintErrorCodeProviders;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, e.getErrorCodeName(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), messageOf(fe)))
                .toList();

        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "입력값을 확인해주세요.", fieldErrors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = e.getName() + "의 형식이 올바르지 않습니다.";
        return badRequest(message, e.getName());
    }

    /** 필수 쿼리 파라미터 누락 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return badRequest(e.getParameterName() + "은(는) 필수 값입니다.", e.getParameterName());
    }

    /** 요청 바디가 없거나 JSON 형식·타입(잘못된 enum 값 포함)이 맞지 않음 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "요청 본문을 읽을 수 없습니다. JSON 형식과 값을 확인해주세요."));
    }

    /**
     * 사전 검사를 통과한 동시 요청이 DB 제약에서 막힌 경우. 유니크 제약은 도메인 오류 코드로, 그 밖은 409 CONFLICT로 응답한다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        Optional<DomainErrorCode> domainErrorCode = findConstraintErrorCode(e);
        if (domainErrorCode.isPresent()) {
            DomainErrorCode code = domainErrorCode.get();
            return ResponseEntity.status(code.getErrorCode().getStatus())
                    .body(ErrorResponse.of(code.getErrorCode(), code.name(), code.getDefaultMessage()));
        }
        log.warn("Unmapped data integrity violation: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
        return ResponseEntity.status(ErrorCode.CONFLICT.getStatus())
                .body(ErrorResponse.of(ErrorCode.CONFLICT, DATA_CONFLICT_MESSAGE));
    }

    /** 잠금 대기 시간 초과·교착 등으로 트랜잭션이 거절된 경우. 재시도하면 성공할 수 있는 충돌이다. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleLockFailure(PessimisticLockingFailureException e) {
        log.warn("Pessimistic locking failure: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.CONFLICT.getStatus())
                .body(ErrorResponse.of(ErrorCode.CONFLICT, LOCK_CONFLICT_MESSAGE));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getDefaultMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }

    private ResponseEntity<ErrorResponse> badRequest(String message, String field) {
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, message,
                        List.of(new ErrorResponse.FieldError(field, message))));
    }

    /**
     * 원인 예외 메시지에 들어 있는 제약 조건 이름으로 오류 코드를 찾는다.
     * 이름이 다른 이름을 포함하는 경우를 대비해 긴 이름부터 비교한다.
     */
    private Optional<DomainErrorCode> findConstraintErrorCode(DataIntegrityViolationException e) {
        String causeMessage = NestedExceptionUtils.getMostSpecificCause(e).getMessage();
        if (causeMessage == null) {
            return Optional.empty();
        }
        String normalized = causeMessage.toLowerCase(Locale.ROOT);
        return constraintErrorCodeProviders.orderedStream()
                .flatMap(provider -> provider.constraintErrorCodes().entrySet().stream())
                .sorted(Comparator.comparingInt((Map.Entry<String, DomainErrorCode> entry) -> entry.getKey().length())
                        .reversed())
                .filter(entry -> normalized.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private String messageOf(FieldError fieldError) {
        return fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "값을 확인해주세요.";
    }
}
