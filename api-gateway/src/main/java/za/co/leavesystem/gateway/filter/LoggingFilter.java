// LoggingFilter.java : logs all incoming requests and outgoing responses through the gateway
package za.co.leavesystem.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            long startTime = Instant.now().toEpochMilli();
            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getPath().toString();
            String clientIp = resolveClientIp(exchange.getRequest());

            log.info("GATEWAY REQUEST: method={} path={} ip={}", method, path, clientIp);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = Instant.now().toEpochMilli() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value() : 0;
                log.info("GATEWAY RESPONSE: method={} path={} status={} duration={}ms",
                        method, path, statusCode, duration);
            }));
        };
    }

    // Pure function: extracts the real client IP from request headers
    private String resolveClientIp(org.springframework.http.server.reactive.ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    public static class Config {}
}
