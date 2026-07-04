package skinemsya.vse.ru.app.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import skinemsya.vse.ru.app.infrastructure.config.WebMvcTestSecurityConfig;
import skinemsya.vse.ru.app.infrastructure.web.CorrelationIdFilter;
import skinemsya.vse.ru.auth.application.JwtTokenService;
import skinemsya.vse.ru.common.domain.CorrelationId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ExceptionHandlerTestController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        }
)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class, WebMvcTestSecurityConfig.class})
@ActiveProfiles({"test", "webmvc-test"})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    void shouldReturnNotFoundForUnknownEndpoint() throws Exception {
        mockMvc.perform(get("/unknown-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"))
                .andExpect(header().exists(CorrelationId.HEADER_NAME));
    }

    @Test
    void shouldReturnValidationError() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields[0].field").value("name"));
    }

    @Test
    void shouldReturnDomainError() throws Exception {
        mockMvc.perform(get("/test/domain-error"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Entity missing"));
    }

    @Test
    void shouldPropagateProvidedCorrelationId() throws Exception {
        mockMvc.perform(get("/unknown-endpoint")
                        .header(CorrelationId.HEADER_NAME, "test-correlation-id"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(CorrelationId.HEADER_NAME, "test-correlation-id"))
                .andExpect(jsonPath("$.correlationId").value("test-correlation-id"));
    }
}
