package skinemsya.vse.ru.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void shouldMapValidationErrorToBadRequest() {
        assertThat(ErrorCode.VALIDATION_ERROR.httpStatus()).isEqualTo(400);
        assertThat(ErrorCode.VALIDATION_ERROR.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(ErrorCode.VALIDATION_ERROR.severity()).isEqualTo(ErrorSeverity.LOW);
    }

    @Test
    void shouldMapInternalErrorToServerError() {
        assertThat(ErrorCode.INTERNAL_ERROR.httpStatus()).isEqualTo(500);
        assertThat(ErrorCode.INTERNAL_ERROR.severity()).isEqualTo(ErrorSeverity.CRITICAL);
    }

    @Test
    void shouldMapAuthorizationErrorToForbidden() {
        assertThat(ErrorCode.AUTHORIZATION_ERROR.httpStatus()).isEqualTo(403);
        assertThat(ErrorCode.AUTHORIZATION_ERROR.severity()).isEqualTo(ErrorSeverity.NONE);
    }
}
