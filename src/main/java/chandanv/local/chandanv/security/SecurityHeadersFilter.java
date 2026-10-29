package chandanv.local.chandanv.security;

import java.io.IOException;

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
 * Header bảo mật cho mọi response:
 *  - Content-Security-Policy: chỉ cho tải script/style/font từ chính server và 2 CDN đang dùng,
 *    chặn nhúng trang vào iframe (chống clickjacking), không cho gửi form ra ngoài.
 *  - X-Content-Type-Options, Referrer-Policy, Permissions-Policy, X-Frame-Options.
 *  - Strict-Transport-Security khi chạy HTTPS và bật app.security.hsts (mặc định tắt, xem bên dưới).
 *  - API không cho trình duyệt/proxy lưu cache (dữ liệu đơn hàng, token).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String CSP = String.join("; ",
            "default-src 'self'",
            // 'unsafe-inline' cần cho các <script> và onclick hiện có trong HTML
            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com",
            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com",
            "font-src 'self' data: https://cdn.jsdelivr.net https://fonts.gstatic.com",
            "img-src 'self' data: blob: https:",
            "worker-src 'self' blob:", // canvas-confetti tạo Web Worker từ blob:
            // localhost:* -> mở trang bằng Live Server / cổng khác khi dev vẫn gọi được API (không ghi cứng cổng)
            "connect-src 'self' ws: wss: https://localhost:* https://127.0.0.1:* http://localhost:* http://127.0.0.1:*",
            "frame-ancestors 'none'",
            "form-action 'self'",
            "base-uri 'self'",
            "object-src 'none'");

    /**
     * HSTS (Strict-Transport-Security) bắt trình duyệt nhớ "địa chỉ này chỉ được vào bằng HTTPS" trong 1 năm.
     * Mặc định TẮT vì với project này nó gây hại nhiều hơn lợi:
     *  - Chứng chỉ ở đây là tự ký. Khi đã nhận HSTS, Chrome KHÔNG cho bấm "Nâng cao -> Tiếp tục" qua cảnh báo
     *    chứng chỉ nữa -> máy đó không vào được trang (cho tới khi xoá HSTS trong chrome://net-internals/#hsts).
     *  - Chạy lại bằng HTTP (APP_HTTPS=false) thì trình duyệt vẫn tự ép sang https -> không mở được.
     * Chỉ bật (app.security.hsts=true) khi chạy thật với tên miền và chứng chỉ do tổ chức uy tín cấp.
     */
    private final boolean hsts;

    public SecurityHeadersFilter(@Value("${app.security.hsts:false}") boolean hsts) {
        this.hsts = hsts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", CSP);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=()");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        if (hsts && request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000");
        }
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(request, response);
    }
}
