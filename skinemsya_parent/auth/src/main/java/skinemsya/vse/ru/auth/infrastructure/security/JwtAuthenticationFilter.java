package skinemsya.vse.ru.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import skinemsya.vse.ru.auth.application.JwtTokenService;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.security.AuthenticatedUser;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            var token = authorization.substring(7);
            try {
                long userId = jwtTokenService.parseUserId(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(userId), null, new AuthenticatedUser(userId).getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (DomainException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
