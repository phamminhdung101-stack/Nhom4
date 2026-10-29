package chandanv.local.chandanv.config;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Cho chữ tiếng Việt trong log hiện đúng trên console Windows (gọi đầu tiên trong main).
 *
 * Vấn đề: Spring Boot ghi log ra console bằng bảng mã của console (System.console().charset()). Nhiều máy Windows
 * để console ở bảng mã cũ 437 / 1252, không có chữ "ệ, ạ, đ...", nên log hiện "Ti?m Trà ?? ch?y". Máy nào bật
 * sẵn UTF-8 thì không bị, vì vậy lỗi chỉ thấy khi mang project sang máy khác.
 *
 * Cách sửa:
 *  1. Chạy "chcp 65001" để chuyển console đang dùng sang UTF-8. Tiến trình con dùng chung console với app
 *     (kể cả khi chạy qua mvnw spring-boot:run) nên lệnh này đổi được bảng mã của chính cửa sổ terminal.
 *  2. Bảng mã của Java đã chốt lúc JVM khởi động, nên phải báo thêm cho Spring Boot (logging.charset.console)
 *     và System.out ghi theo UTF-8.
 * Terminal giữ UTF-8 sau khi tắt server; việc này vô hại, chữ tiếng Việt của lệnh khác cũng hiện đúng hơn.
 * Chạy trong IDE (không có console thật) thì chcp không làm gì, còn log ghi UTF-8 đúng với cửa sổ Run của IDE.
 */
public final class ConsoleUtf8 {

    /** DevTools gọi lại main mỗi lần khởi động lại: chỉ đổi bảng mã 1 lần trong cùng JVM. */
    private static final String DA_BAT = "tiemtra.console.utf8";

    private ConsoleUtf8() {
    }

    public static void bat() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return;
        if (System.getProperty(DA_BAT) != null) return;
        System.setProperty(DA_BAT, "1");

        Charset hienTai = System.console() != null ? System.console().charset() : Charset.defaultCharset();
        if (StandardCharsets.UTF_8.equals(hienTai)) return;

        try {
            new ProcessBuilder("cmd", "/c", "chcp 65001 >nul 2>&1").inheritIO().start().waitFor();
        } catch (Exception e) {
            // không có cmd / bị chặn: vẫn ghi UTF-8 bên dưới, chỉ console không đổi được bảng mã
        }
        System.setProperty("logging.charset.console", "UTF-8");
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
    }
}
