package za.co.leavesystem.auth.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import za.co.leavesystem.auth.service.JwtService;

import java.io.IOException;

/**
 * Protects /auth/admin/** endpoints: validates JWT and enforces ADMIN role.
 * Runs only for paths matching /auth/admin/.
 */
@Component
public class JwtAdminFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAdminFilter.class);
    private final JwtService jwtService;

    public JwtAdminFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/auth/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authorization token is required");
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtService.extractAllClaims(token);
            String role = (String) claims.get("role");
            if (!"ADMIN".equals(role)) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Admin role required");
                return;
            }
            // Pass the username downstream so controllers can read it
            request.setAttribute("adminUsername", claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT on admin endpoint: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
            return;
        } catch (JwtException e) {
            log.warn("Invalid JWT on admin endpoint: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        chain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\",\"status\":" + status + "}");
    }
}
