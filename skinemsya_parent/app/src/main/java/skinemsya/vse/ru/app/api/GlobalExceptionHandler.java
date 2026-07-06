package skinemsya.vse.ru.app.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import skinemsya.vse.ru.common.api.ApiErrorField;
import skinemsya.vse.ru.common.api.ApiErrorResponse;
import skinemsya.vse.ru.common.domain.CorrelationId;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.domain.TypedDomainException;
import skinemsya.vse.ru.debts.domain.exception.DebtsDomainException;
import skinemsya.vse.ru.events.domain.exception.EventsDomainException;
import skinemsya.vse.ru.files.domain.exception.FilesDomainException;
import skinemsya.vse.ru.groups.domain.exception.GroupsDomainException;
import skinemsya.vse.ru.payments.domain.exception.PaymentsDomainException;
import skinemsya.vse.ru.receipts.domain.exception.ReceiptsDomainException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
        GroupsDomainException.class,
        EventsDomainException.class,
        FilesDomainException.class,
        ReceiptsDomainException.class,
        PaymentsDomainException.class,
        DebtsDomainException.class
    })
    public ResponseEntity<ApiErrorResponse> handleModuleDomainException(
            TypedDomainException ex, HttpServletRequest request) {
        return toErrorResponse(ex, request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        return toErrorResponse(ex, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(ApiErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR.code(), "Validation failed", correlationId(request), fields));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.httpStatus())
                .body(ApiErrorResponse.of(ErrorCode.NOT_FOUND.code(), "Resource not found", correlationId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.httpStatus())
                .body(ApiErrorResponse.of(
                        ErrorCode.INTERNAL_ERROR.code(), "Internal server error", correlationId(request)));
    }

    private ApiErrorField toFieldError(FieldError fieldError) {
        return new ApiErrorField(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiErrorResponse> toErrorResponse(TypedDomainException ex, HttpServletRequest request) {
        var errorCode = ex.errorCode();
        log.warn("Domain error [{}]: {}", errorCode.code(), ex.getMessage());
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiErrorResponse.of(errorCode.code(), ex.getMessage(), correlationId(request)));
    }

    private String correlationId(HttpServletRequest request) {
        var fromRequest = request.getHeader(CorrelationId.HEADER_NAME);
        if (fromRequest != null && !fromRequest.isBlank()) {
            return fromRequest;
        }
        var fromMdc = MDC.get(CorrelationId.HEADER_NAME);
        return fromMdc != null ? fromMdc : "unknown";
    }
}
