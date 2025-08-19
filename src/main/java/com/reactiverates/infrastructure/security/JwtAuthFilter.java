package com.reactiverates.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthFilter implements WebFilter {
    
    private final JwtService jwtService;
    
    private final ServerSecurityContextRepository securityContextRepository = 
        new WebSessionServerSecurityContextRepository();
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        if (shouldNotFilter(exchange)) {
            return chain.filter(exchange);
        }
        
        return securityContextRepository.load(exchange)
            .flatMap(context -> {
                if (context != null && context.getAuthentication() != null) {
                    return chain.filter(exchange);
                }
                return processAuthentication(exchange, chain);
            })
            .switchIfEmpty(processAuthentication(exchange, chain));
    }
    
    private Mono<Void> processAuthentication(ServerWebExchange exchange, WebFilterChain chain) {
        try {
            String token = extractTokenFromRequest(exchange);
            
            if (token != null) {
                return authenticateUser(token, exchange)
                    .flatMap(context -> securityContextRepository.save(exchange, context))
                    .then(chain.filter(exchange))
                    .onErrorResume(e -> {
                        log.error("Authentication error: {}", e.getMessage());
                        return chain.filter(exchange);
                    });
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        
        return chain.filter(exchange);
    }
    
    private String extractTokenFromRequest(ServerWebExchange exchange) {
        String bearerToken = exchange.getRequest()
            .getHeaders()
            .getFirst(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private Mono<SecurityContext> authenticateUser(String token, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            try {
                if (jwtService.validateToken(token)) {
                    String username = jwtService.extractUsername(token);
                    
                    List<String> roles = jwtService.extractClaim(token, 
                        claims -> claims.get("roles", List.class));
                    
                    Collection<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role))
                        .toList();
                    
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                    
                    SecurityContext context = new SecurityContextImpl(authToken);
                    
                    log.debug("User {} authenticated successfully with authorities: {}", username, authorities);
                    return context;
                }
            } catch (Exception e) {
                log.debug("JWT token validation failed: {}", e.getMessage());
            }
            return null;
        }).flatMap(context -> {
            if (context != null) {
                return Mono.just(context);
            }
            return Mono.empty();
        });
    }
    
    private boolean shouldNotFilter(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        
        return path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/api-docs/") ||
               path.startsWith("/swagger-resources/") ||
               path.startsWith("/webjars/");
    }
}