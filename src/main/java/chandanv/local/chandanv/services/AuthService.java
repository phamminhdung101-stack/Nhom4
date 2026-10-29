package chandanv.local.chandanv.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.models.entity.ThietBi;
import chandanv.local.chandanv.repositories.AdminRepository;
import chandanv.local.chandanv.repositories.BanAnRepository;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Xác thực đơn giản bằng token:
 *  - Nhân viên: header X-Admin-Token (nhận khi đăng nhập).
 *  - Khách tại bàn: mã in trong QR chỉ dùng để XIN VÀO bàn (POST /api/ban-an/{id}/vao-ban);
 *    mọi thao tác khác dùng header X-Table-Token = mã riêng của điện thoại đã được duyệt trong phiên bàn.
 *    Bàn về Trống là phiên kết thúc -> mã của các máy cũ hết hiệu lực.
 */
@Service
public class AuthService {

    public static final String ADMIN_HEADER = "X-Admin-Token";
    public static final String TABLE_HEADER = "X-Table-Token";

    private final AdminRepository adminRepository;
    private final BanAnRepository banAnRepository;

    public AuthService(AdminRepository adminRepository, BanAnRepository banAnRepository) {
        this.adminRepository = adminRepository;
        this.banAnRepository = banAnRepository;
    }

    public Optional<Admin> timNhanVien(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return adminRepository.findByToken(token.trim())
                .filter(a -> a.getTokenHetHan() != null && a.getTokenHetHan().isAfter(Instant.now()));
    }

    /** Mã in trong QR của bàn – chỉ để xin vào bàn, không đặt món trực tiếp được. */
    public boolean maQRHopLe(int idBan, String qr) {
        if (qr == null || qr.isBlank()) return false;
        return banAnRepository.findByIdBan(idBan)
                .map(b -> bang(b.getToken(), qr))
                .orElse(false);
    }

    /** Điện thoại thuộc phiên bàn hiện tại (kể cả đang chờ duyệt). */
    public Optional<ThietBi> timThietBi(int idBan, String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return banAnRepository.findByIdBan(idBan)
                .flatMap(b -> b.getThietBi().stream().filter(t -> bang(t.getToken(), token)).findFirst());
    }

    /** Máy đã được duyệt trong phiên bàn -> được thao tác (giỏ hàng, đặt món, chat, nhận sự kiện bàn). */
    public boolean tokenBanHopLe(int idBan, String token) {
        return timThietBi(idBan, token).filter(ThietBi::isDaDuyet).isPresent();
    }

    /** Máy của phiên vừa kết thúc (bàn đã trả) – chỉ được xem lại đơn của phiên đó. */
    public boolean laMayPhienCu(int idBan, String token) {
        if (token == null || token.isBlank()) return false;
        return banAnRepository.findByIdBan(idBan)
                .map(b -> b.getPhienCuTokens().stream().anyMatch(t -> bang(t, token)))
                .orElse(false);
    }

    /** Máy của khách đang gọi request (đã duyệt), để ghi "ai đặt đơn". */
    public Optional<ThietBi> thietBiCua(HttpServletRequest request, int idBan) {
        return timThietBi(idBan, request.getHeader(TABLE_HEADER)).filter(ThietBi::isDaDuyet);
    }

    /**
     * So sánh mã bằng MessageDigest.isEqual thay cho String.equals.
     * String.equals dừng ngay ở ký tự sai đầu tiên, nên mã đúng càng nhiều ký tự đầu thì phản hồi càng chậm
     * một chút; kẻ tấn công đo thời gian có thể đoán dần từng ký tự (timing attack). isEqual luôn so hết.
     */
    private static boolean bang(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.trim().getBytes(StandardCharsets.UTF_8));
    }

    public Admin yeuCauNhanVien(HttpServletRequest request) {
        return timNhanVien(request.getHeader(ADMIN_HEADER)).orElseThrow(() -> {
            chandanv.local.chandanv.security.AuditLog.warn("ADMIN_UNAUTHORIZED",
                    "ip=" + chandanv.local.chandanv.security.ClientIp.of(request)
                            + " " + request.getMethod() + " " + request.getRequestURI());
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
        });
    }

    /** Cho phép nếu là nhân viên, hoặc là máy đã được duyệt trong phiên hiện tại của bàn idBan. Không thì 403. */
    public void yeuCauBanHoacNhanVien(HttpServletRequest request, int idBan) {
        if (timNhanVien(request.getHeader(ADMIN_HEADER)).isPresent()) return;
        if (!tokenBanHopLe(idBan, request.getHeader(TABLE_HEADER))) {
            chandanv.local.chandanv.security.AuditLog.warn("TABLE_TOKEN_INVALID",
                    "ban=" + idBan + " ip=" + chandanv.local.chandanv.security.ClientIp.of(request)
                            + " " + request.getMethod() + " " + request.getRequestURI());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Máy của bạn chưa được vào bàn hoặc phiên bàn đã kết thúc, vui lòng quét lại mã QR trên bàn");
        }
    }
}
