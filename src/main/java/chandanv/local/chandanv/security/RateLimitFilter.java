package chandanv.local.chandanv.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Giới hạn tần suất & kích thước request cho /api/** theo IP (chống spam, DoS đơn giản, dò mật khẩu).
 * Ngưỡng chỉnh trong application.properties (app.security.rate-limit.*), mặc định nới rộng cho demo/đồ án:
 *  - Mọi request API: 1000 / phút / IP
 *  - Request ghi (POST/PUT/PATCH/DELETE): 300 / phút / IP
 *  - Đăng nhập: 20 / phút / IP
 *  - Body > 1 MB: luôn từ chối (413)
 * Tắt hẳn giới hạn tần suất (vd. khi đo tải): app.security.rate-limit.enabled=false
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    static final long WINDOW_MS = 60_000;
    static final long MAX_BODY_BYTES = 1024 * 1024;

    private final RateLimiter limiter;
    private final boolean enabled;
    private final int limitAll;
    private final int limitWrite;
    private final int limitLogin;

    public RateLimitFilter(RateLimiter limiter,
                           @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
                           @Value("${app.security.rate-limit.requests-per-minute:1000}") int limitAll,
                           @Value("${app.security.rate-limit.writes-per-minute:300}") int limitWrite,
                           @Value("${app.security.rate-limit.logins-per-minute:20}") int limitLogin) {
        this.limiter = limiter;
        this.enabled = enabled;
        this.limitAll = limitAll;
        this.limitWrite = limitWrite;
        this.limitLogin = limitLogin;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = ClientIp.of(request);
        String method = request.getMethod();

        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            AuditLog.warn("BODY_TOO_LARGE", ip + " " + method + " " + request.getRequestURI());
            reject(response, 413, "Dữ liệu gửi lên quá lớn", 0);
            return;
        }
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        String key;
        int limit;
        if (request.getRequestURI().equals("/api/admin/login")) {
            key = "login:" + ip;
            limit = limitLogin;
        } else if (!"GET".equals(method) && !"HEAD".equals(method)) {
            key = "write:" + ip;
            limit = limitWrite;
        } else {
            key = "all:" + ip;
            limit = limitAll;
        }
        if (!limiter.tryAcquire(key, limit, WINDOW_MS) || (!key.startsWith("all:") && !limiter.tryAcquire("all:" + ip, limitAll, WINDOW_MS))) {
            long retry = limiter.secondsUntilReset(key, WINDOW_MS);
            AuditLog.warn("RATE_LIMIT", ip + " " + method + " " + request.getRequestURI());
            reject(response, 429, "Bạn thao tác quá nhanh, vui lòng thử lại sau " + retry + " giây", retry);
            return;
        }
        chain.doFilter(request, response);
    }

    private static void reject(HttpServletResponse response, int status, String message, long retryAfter) throws IOException {
        response.setStatus(status);
        if (retryAfter > 0) response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\",\"time\":\"" + Instant.now() + "\"}");
    }
}
