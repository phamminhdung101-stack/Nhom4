package chandanv.local.chandanv.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nhật ký sự kiện bảo mật (logger tên "AUDIT"): đăng nhập sai, bị khoá, request không đủ quyền, spam WebSocket,
 * máy vào / bị mời ra khỏi bàn, huỷ đơn... Ghi ra console và logs/app.log. Lọc riêng: tìm chữ "AUDIT" trong file.
 */
public final class AuditLog {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    private AuditLog() {}

    public static void info(String event, String detail) {
        log.info("[{}] {}", event, detail);
    }

    public static void warn(String event, String detail) {
        log.warn("[{}] {}", event, detail);
    }
}
