package skinemsya.vse.ru.app.infrastructure.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@Profile("webmvc-test")
public class WebMvcTestSecurityConfig {

    @Bean
    SecurityFilterChain webMvcTestSecurityFilterChain(HttpSecurity http) throws Exception {
        // Stateless test security stub; CSRF tokens are not sent in @WebMvcTest requests.
        return http.csrf(AbstractHttpConfigurer::disable) // lgtm[java/spring-disabled-csrf-protection]
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
