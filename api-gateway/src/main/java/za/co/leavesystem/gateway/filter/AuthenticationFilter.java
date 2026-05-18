// AuthenticationFilter.java : validates JWT tokens before routing requests to backend services
package za.co.leavesystem.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or malformed Authorization header for path: {}",
                        exchange.getRequest().getPath());
                return buildErrorResponse(exchange.getResponse(), HttpStatus.UNAUTHORIZED,
                        "{\"error\":\"Authorization token is required\",\"status\":401}");
            }

            String token = authHeader.substring(7);
            try {
                Claims claims = parseToken(token);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                log.debug("Request authorised: user={} role={} path={}",
                        username, role, exchange.getRequest().getPath());

                // Forward user info to downstream services via headers
                var mutatedRequest = exchange.getRequest().mutate()
                        .header("X-Auth-Username", username)
                        .header("X-Auth-Role", role)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (Exception e) {
                log.warn("JWT validation failed: {} for path: {}", e.getMessage(),
                        exchange.getRequest().getPath());
                return buildErrorResponse(exchange.getResponse(), HttpStatus.UNAUTHORIZED,
                        "{\"error\":\"Invalid or expired token\",\"status\":401}");
            }
        };
    }

    // Pure function: parses and validates the JWT token using the shared secret
    private Claims parseToken(String token) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    // Output logic: builds a JSON error response with the given status and body
    private Mono<Void> buildErrorResponse(org.springframework.http.server.reactive.ServerHttpResponse response,
                                          HttpStatus status, String body) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {}
}
