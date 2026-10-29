package chandanv.local.chandanv.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chuẩn bị HTTPS trước khi Spring khởi động (gọi trong main):
 *  - Tự tạo chứng chỉ tự ký ssl/tiemtra.p12 nếu chưa có, sắp hết hạn, hoặc không chứa IP Wi-Fi hiện tại
 *    (đổi mạng Wi-Fi -> IP đổi -> tạo lại), bằng keytool có sẵn trong JDK (không cần cài thêm / PATH).
 *  - Tắt HTTPS (APP_HTTPS=false) hoặc không tạo được chứng chỉ -> chạy HTTP ở cổng 8081.
 * Phải chạy TRƯỚC Spring vì Tomcat đọc file chứng chỉ ngay lúc mở cổng: thiếu file là server không khởi động được.
 * IP Wi-Fi phải có trong chứng chỉ (SAN): nếu không, trình duyệt báo chứng chỉ "sai địa chỉ" và điện thoại
 * mở bằng IP sẽ gặp lỗi khó qua hơn cả lỗi tự ký.
 */
public final class ChungChiHttps {

    private static final Logger log = LoggerFactory.getLogger(ChungChiHttps.class);
    private static final Path KEYSTORE = Path.of("ssl", "tiemtra.p12");
    private static final String ALIAS = "tiemtra";

    private ChungChiHttps() {}

    public static void chuanBi() {
        if ("false".equalsIgnoreCase(env("APP_HTTPS", "true"))) {
            chayHttp("APP_HTTPS=false");
            return;
        }
        String matKhau = env("SSL_PASSWORD", "tiemtra123");
        String ip = LanBrowserLauncher.timIpLan().orElse(null);
        try {
            if (!conDung(matKhau, ip)) {
                taoChungChi(matKhau, ip);
                log.info("Đã tạo chứng chỉ HTTPS tự ký {} cho localhost, 127.0.0.1{}", KEYSTORE, ip == null ? "" : ", " + ip);
            }
        } catch (Exception e) {
            log.warn("Không tạo được chứng chỉ HTTPS ({}) -> chạy HTTP", e.getMessage());
            chayHttp("thiếu chứng chỉ");
        }
    }

    /** Chứng chỉ hiện có còn dùng được: mở được, còn hạn > 30 ngày, có IP Wi-Fi hiện tại. */
    private static boolean conDung(String matKhau, String ip) {
        if (!Files.exists(KEYSTORE)) return false;
        try (InputStream in = Files.newInputStream(KEYSTORE)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(in, matKhau.toCharArray());
            X509Certificate cert = (X509Certificate) ks.getCertificate(ALIAS);
            if (cert == null) return false;
            cert.checkValidity(Date.from(Instant.now().plus(Duration.ofDays(30))));
            if (ip == null) return true;
            Collection<List<?>> san = cert.getSubjectAlternativeNames();
            return san != null && san.stream().anyMatch(s -> Integer.valueOf(7).equals(s.get(0)) && ip.equals(s.get(1)));
        } catch (Exception e) {
            return false; // hỏng / sai mật khẩu / hết hạn -> tạo lại
        }
    }

    private static void taoChungChi(String matKhau, String ip) throws IOException, InterruptedException {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", windows ? "keytool.exe" : "keytool");
        if (!Files.exists(keytool)) throw new IOException("không thấy keytool trong JDK");
        Files.createDirectories(KEYSTORE.getParent());
        Files.deleteIfExists(KEYSTORE);

        List<String> cmd = new ArrayList<>(List.of(keytool.toString(), "-genkeypair", "-alias", ALIAS,
                "-keyalg", "RSA", "-keysize", "2048", "-validity", "825",
                "-storetype", "PKCS12", "-keystore", KEYSTORE.toString(), "-storepass", matKhau,
                "-dname", "CN=Tiem Tra, OU=Nhom4, O=Nhom4, C=VN",
                "-ext", "SAN=dns:localhost,ip:127.0.0.1" + (ip == null ? "" : ",ip:" + ip)));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!p.waitFor(60, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IOException("keytool lỗi: " + out);
        }
    }

    /** Chạy HTTP: tắt SSL, cổng 8081 (trừ khi đã đặt PORT), không mở cổng chuyển hướng. */
    private static void chayHttp(String lyDo) {
        System.setProperty("server.ssl.enabled", "false");
        if (System.getenv("PORT") == null) System.setProperty("server.port", "8081");
        System.setProperty("app.http-port", "0");
        log.info("Chạy HTTP ({})", lyDo);
    }

    private static String env(String key, String macDinh) {
        String v = System.getenv(key);
        // trim: CMD "set APP_HTTPS=false && ..." lưu cả dấu cách cuối ("false ")
        return v == null || v.isBlank() ? macDinh : v.trim();
    }
}
