package skinemsya.vse.ru.impl.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.security.AuthenticatedUser;
import skinemsya.vse.ru.impl.service.auth.JwtTokenService;

/** Аутентифицирует запрос по JWT из заголовка Authorization. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    /** Проверяет Bearer-токен и продолжает цепочку фильтров. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            var token = authorization.substring(7);
            try {
                long userId = jwtTokenService.parseUserId(token);
                var authenticatedUser = new AuthenticatedUser(userId);
                var authentication = new UsernamePasswordAuthenticationToken(
                        authenticatedUser, null, authenticatedUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (DomainException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
