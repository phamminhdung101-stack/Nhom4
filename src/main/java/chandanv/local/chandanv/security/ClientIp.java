package chandanv.local.chandanv.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIp {

    private ClientIp() {}

    /**
     * IP của client. Không tin header X-Forwarded-For (có thể bị giả mạo).
     * Ngoại lệ: Cloudflare Tunnel. cloudflared chạy trên chính máy này nên mọi khách đều có IP loopback,
     * giới hạn request và khoá đăng nhập sẽ tính gộp tất cả. Vì vậy chỉ tin CF-Connecting-IP khi request
     * đến từ loopback: máy khác trong mạng kết nối thẳng có IP thật, không giả được header này.
     */
    public static String of(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            String quaTunnel = request.getHeader("CF-Connecting-IP");
            if (quaTunnel != null && !quaTunnel.isBlank()) return quaTunnel.trim();
        }
        return ip == null ? "unknown" : ip;
    }
}
