package chandanv.local.chandanv.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Server chạy xong -> tự tìm IP Wi-Fi/LAN của máy, in đường dẫn ra log và mở trình duyệt
 * vào trang quản trị bằng IP đó (mã QR in ra sẽ trỏ đúng IP để điện thoại cùng Wi-Fi quét được).
 * Tắt tự mở trình duyệt: app.open-browser=false (biến môi trường APP_OPEN_BROWSER=false).
 *
 * Máy đã cài cloudflared thì tự bật thêm Cloudflare Tunnel và mở trình duyệt bằng địa chỉ tunnel, để điện thoại
 * khác Wi-Fi / 4G quét QR được. Chưa cài thì chạy như cũ. Tắt: app.tunnel=false (biến môi trường APP_TUNNEL=false).
 */
@Component
public class LanBrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(LanBrowserLauncher.class);

    /** DevTools tự khởi động lại khi sửa code -> không mở thêm tab (System property còn nguyên trong cùng JVM). */
    private static final String OPENED_FLAG = "tiemtra.browser.opened";

    /** Card mạng ảo (VMware, VirtualBox, WSL, Docker, VPN...) – IP của chúng điện thoại không truy cập được. */
    private static final List<String> CARD_AO = List.of(
            "virtual", "vmware", "vmnet", "vbox", "hyper-v", "vethernet", "wsl", "docker",
            "warp", "wiresock", "wireguard", "wintun", "tap-", "tunnel", "zerotier", "tailscale",
            "hamachi", "bluetooth", "loopback");

    private static final List<String> CARD_WIFI = List.of("wi-fi", "wifi", "wlan", "wireless", "802.11");

    /** Bắt buộc có dấu "-": dòng báo lỗi của cloudflared cũng có https://api.trycloudflare.com. */
    private static final Pattern DIA_CHI_TUNNEL = Pattern.compile("https://[a-z0-9]+(-[a-z0-9]+)+\\.trycloudflare\\.com");

    /** winget cài vào đây nhưng terminal đang mở chưa nhận PATH mới. */
    private static final String CLOUDFLARED_WINDOWS = "C:\\Program Files (x86)\\cloudflared\\cloudflared.exe";

    private static final Path FILE_PID_TUNNEL = Path.of("logs", "tunnel.pid");

    private final Environment env;
    private final boolean openBrowser;
    private final boolean tunnel;

    public LanBrowserLauncher(Environment env,
                              @Value("${app.open-browser:true}") boolean openBrowser,
                              @Value("${app.tunnel:true}") boolean tunnel) {
        this.env = env;
        this.openBrowser = openBrowser;
        this.tunnel = tunnel;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String port = env.getProperty("local.server.port", env.getProperty("server.port", "8443"));
        boolean https = env.getProperty("server.ssl.enabled", Boolean.class, false);
        String scheme = https ? "https" : "http";
        Optional<String> ip = timIpLan();
        String base = scheme + "://" + ip.orElse("localhost") + ":" + port;
        String httpPort = env.getProperty("app.http-port", "0");
        boolean trungCong = https && httpPort.equals(port); // vd. terminal đang có PORT=8081
        String ghiChuHttps = https
                ? "\n HTTPS/WSS: chứng chỉ tự ký -> lần đầu trình duyệt cảnh báo, bấm Nâng cao -> Tiếp tục."
                  + "\n Thanh địa chỉ ghi \"Not secure\", https bị gạch: bình thường (vẫn mã hoá), xem README.md mục 6."
                  + ("0".equals(httpPort) || trungCong ? "" : "\n http://<IP>:" + httpPort + " (mã QR in trước đây) tự chuyển sang HTTPS.")
                : "";
        if (trungCong) {
            log.warn("HTTPS đang chạy ở cổng {} trùng cổng chuyển hướng -> link / mã QR http://<IP>:{} sẽ lỗi. "
                    + "Có thể terminal đang đặt PORT={}: tắt server, chạy Remove-Item Env:PORT, Env:SERVER_PORT rồi chạy lại "
                    + "(HTTPS về 8443, {} chuyển hướng).", port, port, port, port);
        }

        log.info("""

                ============================================================
                 Tiệm Trà đã chạy!   IP Wi-Fi/LAN: {}
                 Quản trị   : {}/admin
                 Mã QR bàn  : {}/admin/qr_maker.html
                 Khách (QR) : {}/welcome.html?idBan=..&t=..
                 Điện thoại phải dùng CHUNG Wi-Fi với máy này.{}
                ============================================================""",
                ip.orElse("(không tìm thấy – đang dùng localhost)"), base, base, base, ghiChuHttps);
        if (ip.isEmpty()) {
            log.warn("Không tìm thấy IP Wi-Fi/LAN: máy chưa kết nối mạng? Điện thoại sẽ không vào được web.");
        }
        canhBaoDoiIp(ip.orElse(null), base);

        // DevTools khởi động lại: tunnel và tab vẫn còn từ lần đầu, bật lại thì địa chỉ tunnel sẽ đổi.
        if (System.getProperty(OPENED_FLAG) != null) return;
        System.setProperty(OPENED_FLAG, "1");

        if (tunnel && !https) log.info("Không bật tunnel: chế độ HTTP làm WebSocket qua tunnel bị 403.");
        if (tunnel && https && batTunnel(port, base + "/admin")) return; // tab mở sau khi tunnel có địa chỉ
        if (openBrowser) moTrinhDuyet(base + "/admin");
    }

    /** Trả về false nếu máy chưa có cloudflared. Lỗi tunnel không được làm hỏng server. */
    private boolean batTunnel(String port, String adminLan) {
        tatTunnelCu();
        Process p = chayCloudflared(port);
        if (p == null) {
            log.info("Chưa cài cloudflared nên chỉ vào được khi chung Wi-Fi. Cài: winget install --id Cloudflare.cloudflared");
            return false;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(p::destroyForcibly)); // tắt server thì web phải rời Internet
        try {
            Files.createDirectories(FILE_PID_TUNNEL.getParent());
            Files.writeString(FILE_PID_TUNNEL, String.valueOf(p.pid()));
        } catch (IOException e) {
            log.warn("Không ghi được {}: {}", FILE_PID_TUNNEL, e.getMessage());
        }
        Thread doc = new Thread(() -> docTunnel(p, adminLan), "tunnel");
        doc.setDaemon(true);
        doc.start();
        return true;
    }

    /**
     * Đóng terminal đột ngột thì shutdown hook có thể không kịp chạy: cloudflared cũ còn sống và sẽ trỏ vào server
     * mới. Kiểm tra tên tiến trình trước khi tắt, vì Windows có thể cấp lại PID đó cho chương trình khác.
     */
    private static void tatTunnelCu() {
        try {
            ProcessHandle.of(Long.parseLong(Files.readString(FILE_PID_TUNNEL).trim()))
                    .filter(h -> h.info().command().orElse("").toLowerCase(Locale.ROOT).contains("cloudflared"))
                    .ifPresent(ProcessHandle::destroyForcibly);
        } catch (IOException | NumberFormatException e) {
            // chưa có file hoặc file hỏng: không có gì để tắt
        }
    }

    private static Process chayCloudflared(String port) {
        for (String exe : List.of("cloudflared", CLOUDFLARED_WINDOWS)) {
            try {
                return new ProcessBuilder(exe, "tunnel", "--no-autoupdate",
                        "--url", "https://localhost:" + port, "--no-tls-verify")
                        .redirectErrorStream(true).start();
            } catch (IOException e) {
                // không có ở chỗ này: thử chỗ tiếp theo
            }
        }
        return null;
    }

    /** Đọc log cloudflared đến khi nó thoát. Phải đọc hết, không thì bộ đệm đầy làm cloudflared bị treo. */
    private void docTunnel(Process p, String adminLan) {
        String diaChi = null;
        boolean daKetNoi = false;
        try (BufferedReader r = p.inputReader(StandardCharsets.UTF_8)) {
            String dong;
            while ((dong = r.readLine()) != null) {
                Matcher m = DIA_CHI_TUNNEL.matcher(dong);
                if (diaChi == null && m.find()) diaChi = m.group();
                if (diaChi != null && !daKetNoi && dong.contains("Registered tunnel connection")) {
                    daKetNoi = true;
                    log.info("""

                            ============================================================
                             Tunnel đã bật: điện thoại khác Wi-Fi / 4G vào được.
                             Quản trị   : {}/admin
                             Mã QR bàn  : {}/admin/qr_maker.html
                             Địa chỉ đổi mỗi lần chạy server nên phải in lại mã QR.
                            ============================================================""", diaChi, diaChi);
                    if (openBrowser) moTrinhDuyet(diaChi + "/admin");
                }
            }
        } catch (IOException e) {
            // cloudflared đã bị tắt: xử lý chung ở dưới
        }
        if (daKetNoi) {
            log.warn("Tunnel đã tắt, khách khác Wi-Fi không vào được nữa.");
        } else {
            log.warn("Không bật được tunnel (máy có Internet không?). Vẫn dùng được trong cùng Wi-Fi.");
            if (openBrowser) moTrinhDuyet(adminLan);
        }
    }

    /**
     * Mã QR in trên bàn chứa IP của máy chạy server. Đổi Wi-Fi (hoặc router cấp IP khác) -> mã QR đã in trỏ sai
     * -> nhắc in lại. Lần chạy trước lưu ở logs/ip-lan.txt dạng "IP tên-máy".
     * Chỉ cảnh báo khi CÙNG MÁY đổi IP: copy cả thư mục project sang máy khác thì file này đi theo, nhưng máy mới
     * chưa in mã QR nào nên không cần nhắc (bản cũ chỉ lưu IP, không có tên máy -> cũng bỏ qua).
     */
    private static void canhBaoDoiIp(String ipMoi, String base) {
        if (ipMoi == null) return;
        Path file = Path.of("logs", "ip-lan.txt");
        String may = tenMay();
        try {
            String[] cu = Files.exists(file) ? Files.readString(file).trim().split("\\s+", 2) : new String[0];
            String ipCu = cu.length > 0 ? cu[0] : null;
            boolean cungMay = cu.length > 1 && cu[1].equals(may);
            Files.createDirectories(file.getParent());
            Files.writeString(file, ipMoi + " " + may);
            if (cungMay && ipCu != null && !ipCu.isEmpty() && !ipCu.equals(ipMoi)) {
                log.warn("""

                        ************************************************************
                         IP Wi-Fi đã đổi: {} -> {}
                         Mã QR đã in trên bàn đang trỏ IP CŨ, khách quét sẽ không vào được.
                         -> Mở {}/admin/qr_maker.html, bấm "In tất cả" để in lại mã QR.
                         (Tránh phải in lại: đặt IP cố định cho máy này trong router - DHCP reservation)
                        ************************************************************""", ipCu, ipMoi, base);
            }
        } catch (IOException e) {
            log.debug("Không lưu được IP lần chạy trước: {}", e.getMessage());
        }
    }

    private static String tenMay() {
        String ten = System.getenv("COMPUTERNAME");
        if (ten == null || ten.isBlank()) ten = System.getenv("HOSTNAME");
        if (ten == null || ten.isBlank()) {
            try {
                ten = InetAddress.getLocalHost().getHostName();
            } catch (IOException e) {
                ten = "?";
            }
        }
        return ten.trim().replaceAll("\\s+", "_");
    }

    /** Ưu tiên card Wi-Fi, sau đó card LAN thật; chỉ lấy IPv4 mạng nội bộ (192.168.x, 10.x, 172.16-31.x). */
    static Optional<String> timIpLan() {
        String ipWifi = null;
        String ipKhac = null;
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual() || ni.isPointToPoint()) continue;
                String ten = (ni.getName() + " " + ni.getDisplayName()).toLowerCase(Locale.ROOT);
                if (CARD_AO.stream().anyMatch(ten::contains)) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!(a instanceof Inet4Address) || !a.isSiteLocalAddress()) continue;
                    if (ipWifi == null && CARD_WIFI.stream().anyMatch(ten::contains)) ipWifi = a.getHostAddress();
                    else if (ipKhac == null) ipKhac = a.getHostAddress();
                }
            }
        } catch (SocketException e) {
            log.debug("Không đọc được danh sách card mạng: {}", e.getMessage());
        }
        if (ipWifi != null) return Optional.of(ipWifi);
        if (ipKhac != null) return Optional.of(ipKhac);

        // Dự phòng: hỏi hệ điều hành card nào đang ra Internet (UDP connect không gửi gói nào đi)
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(InetAddress.getByName("8.8.8.8"), 80);
            InetAddress a = s.getLocalAddress();
            if (a instanceof Inet4Address && a.isSiteLocalAddress()) return Optional.of(a.getHostAddress());
        } catch (IOException e) {
            log.debug("Không xác định được card mạng mặc định: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /** Spring Boot chạy ở chế độ headless nên không dùng được java.awt.Desktop -> gọi lệnh của hệ điều hành. */
    private static void moTrinhDuyet(String url) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ProcessBuilder pb = os.contains("win") ? new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url)
                : os.contains("mac") ? new ProcessBuilder("open", url)
                : new ProcessBuilder("xdg-open", url);
        try {
            pb.start();
        } catch (IOException e) {
            log.info("Không tự mở được trình duyệt, hãy mở thủ công: {}", url);
        }
    }
}
