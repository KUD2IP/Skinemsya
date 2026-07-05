package skinemsya.vse.ru.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    void shouldExposeErrorCode() {
        var exception = new DomainException(ErrorCode.NOT_FOUND, "User not found");

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("User not found");
    }

    @Test
    void shouldSupportCause() {
        var cause = new RuntimeException("db");
        var exception = new DomainException(ErrorCode.INTERNAL_ERROR, "Failed", cause);

        assertThat(exception.getCause()).isSameAs(cause);
    }
}
