// RateLimitFilter.java : enforces rate limiting on login endpoint using a custom token bucket
package za.co.leavesystem.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000L;

    // Tracks request counts and window-start timestamps per IP
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> windowStart = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().contains("/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        if (isRateLimited(ip)) {
            log.warn("Rate limit exceeded for IP: {}", ip);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many login attempts. Please try again in one minute.\",\"status\":429}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Pure function: checks and updates the sliding window counter for the given IP
    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        windowStart.putIfAbsent(ip, new AtomicLong(now));
        requestCounts.putIfAbsent(ip, new AtomicInteger(0));

        long start = windowStart.get(ip).get();
        if (now - start > WINDOW_MS) {
            windowStart.get(ip).set(now);
            requestCounts.get(ip).set(1);
            return false;
        }

        int count = requestCounts.get(ip).incrementAndGet();
        return count > MAX_REQUESTS;
    }

    // Pure function: extracts the real client IP, accounting for proxies
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
