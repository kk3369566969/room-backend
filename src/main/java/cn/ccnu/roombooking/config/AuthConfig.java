package cn.ccnu.roombooking.config;

import cn.ccnu.roombooking.common.AuthContext;
import cn.ccnu.roombooking.domain.AuthSessionRepository;
import cn.ccnu.roombooking.domain.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
public class AuthConfig {
  @Bean FilterRegistrationBean<OncePerRequestFilter> authFilter(UserRepository users, AuthSessionRepository sessions, AuthContext context) {
    var bean = new FilterRegistrationBean<OncePerRequestFilter>();
    bean.setFilter(new OncePerRequestFilter() {
      @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
          String token = authorization.substring("Bearer ".length()).trim();
          if (!token.isBlank()) {
            sessions.findByTokenHash(cn.ccnu.roombooking.service.AuthService.hash(token)).ifPresent(session -> {
              if (session.getRevokedAt() == null && session.getExpiresAt().isAfter(LocalDateTime.now())) {
                context.setSession(session);
                context.setExternalId(session.getExternalId());
                users.findByExternalId(session.getExternalId()).ifPresent(context::setUser);
              }
            });
          }
        } else {
          String external = request.getHeader("X-User-Id");
          if (external != null && !external.isBlank()) context.setExternalId(external);
          if (external != null && !external.isBlank()) users.findByExternalId(external).ifPresent(context::setUser);
        }
        chain.doFilter(request, response);
      }
    });
    bean.setOrder(1); return bean;
  }
}
