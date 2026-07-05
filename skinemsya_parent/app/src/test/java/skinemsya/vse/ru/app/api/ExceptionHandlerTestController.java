package skinemsya.vse.ru.app.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;

@RestController
@Validated
class ExceptionHandlerTestController {

    @PostMapping("/test/validate")
    void validate(@Valid @RequestBody ValidationRequest request) {}

    @GetMapping("/test/domain-error")
    void domainError() {
        throw new DomainException(ErrorCode.NOT_FOUND, "Entity missing");
    }

    record ValidationRequest(@NotBlank String name) {}
}
