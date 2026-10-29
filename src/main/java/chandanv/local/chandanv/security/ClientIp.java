package chandanv.local.chandanv.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIp {

    private ClientIp() {}

    /**
     * IP của client. Không tin header X-Forwarded-For (có thể bị giả mạo) vì app chạy trực tiếp,
     * không qua reverse proxy.
     */
    public static String of(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }
}
