package chandanv.local.chandanv.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.repositories.AdminRepository;
import chandanv.local.chandanv.util.TextUtil;

@Service
public class AdminService {

    /** BCrypt của một chuỗi ngẫu nhiên – chỉ dùng để cân bằng thời gian xử lý. */
    private static final String DUMMY_HASH = "$2a$10$b2zzoc9p9mFltSaQcPny2.TejDJCqgecdqRstlyM1gtc7r4QM1ZVe";

    private final AdminRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Duration tokenTtl;

    public AdminService(AdminRepository repository,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.auth.token-ttl-hours:12}") long tokenTtlHours) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenTtl = Duration.ofHours(tokenTtlHours);
    }

    public Admin dangNhap(String username, String matKhau) {
        if (username == null || username.isBlank() || matKhau == null || matKhau.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập tài khoản và mật khẩu");
        }
        Admin admin = repository.findByUsername(username.trim()).orElse(null);
        if (admin == null) {
            // Vẫn chạy BCrypt với hash giả để thời gian phản hồi giống khi tài khoản tồn tại
            // (không cho kẻ tấn công đoán tài khoản nào có thật dựa vào tốc độ).
            passwordEncoder.matches(matKhau, DUMMY_HASH);
        }
        if (admin == null || !kiemTraMatKhau(admin, matKhau)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu!");
        }
        // Token còn hạn -> dùng lại (chỉ gia hạn). Nếu cấp token mới, máy đã đăng nhập trước đó
        // (vd. màn hình bếp) bị đá ra khi cùng tài khoản đăng nhập ở máy khác (vd. POS).
        boolean conHan = admin.getToken() != null && admin.getTokenHetHan() != null
                && admin.getTokenHetHan().isAfter(Instant.now());
        if (!conHan) admin.setToken(TextUtil.randomToken(24));
        admin.setTokenHetHan(Instant.now().plus(tokenTtl));
        admin.setThoiGianDangNhap(new Date());
        return repository.save(admin);
    }

    public void dangXuat(Admin admin) {
        admin.setToken(null);
        admin.setTokenHetHan(null);
        admin.setThoiGianDangXuat(new Date());
        repository.save(admin);
    }

    /** Băm BCrypt các mật khẩu còn lưu dạng chữ thường (dữ liệu cũ). Trả về số tài khoản đã băm. */
    public int bamMatKhauCu() {
        int count = 0;
        for (Admin a : repository.findAll()) {
            String pw = a.getMatKhau();
            if (pw != null && !laBcrypt(pw)) {
                a.setMatKhau(passwordEncoder.encode(pw));
                repository.save(a);
                count++;
            }
        }
        return count;
    }

    private boolean kiemTraMatKhau(Admin admin, String raw) {
        String stored = admin.getMatKhau();
        if (stored == null) return false;
        if (laBcrypt(stored)) {
            return passwordEncoder.matches(raw, stored);
        }
        // Dữ liệu cũ chưa băm: so sánh an toàn thời gian rồi nâng cấp lên BCrypt.
        boolean ok = MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), raw.getBytes(StandardCharsets.UTF_8));
        if (ok) {
            admin.setMatKhau(passwordEncoder.encode(raw));
        }
        return ok;
    }

    static boolean laBcrypt(String s) {
        return s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$");
    }
}
