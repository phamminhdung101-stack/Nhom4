package chandanv.local.chandanv.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class TextUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TextUtil() {}

    /**
     * Cắt khoảng trắng, bỏ ký tự điều khiển (trừ xuống dòng) và giới hạn độ dài.
     * Trả về null nếu chuỗi rỗng. KHÔNG escape HTML – việc hiển thị an toàn do client (textContent) đảm nhận.
     */
    public static String lamSach(String s, int maxLength) {
        if (s == null) return null;
        String t = s.replaceAll("[\\p{Cntrl}&&[^\\n]]", "").trim();
        if (t.isEmpty()) return null;
        return t.length() > maxLength ? t.substring(0, maxLength) : t;
    }

    /** Chuỗi ngẫu nhiên an toàn, dạng base64 URL-safe. */
    public static String randomToken(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
