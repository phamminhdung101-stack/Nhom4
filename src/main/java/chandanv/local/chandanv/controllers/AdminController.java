package chandanv.local.chandanv.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.security.AuditLog;
import chandanv.local.chandanv.security.ClientIp;
import chandanv.local.chandanv.security.LoginAttemptService;
import chandanv.local.chandanv.services.AdminService;
import chandanv.local.chandanv.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;
    private final LoginAttemptService loginAttempts;

    public AdminController(AdminService adminService, AuthService authService, LoginAttemptService loginAttempts) {
        this.adminService = adminService;
        this.authService = authService;
        this.loginAttempts = loginAttempts;
    }

    /** Body dạng form: username=...&matKhau=... -> trả token dùng cho header X-Admin-Token. */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username, @RequestParam String matKhau,
                                     HttpServletRequest request) {
        String ip = ClientIp.of(request);
        loginAttempts.kiemTraKhoa(username, ip);

        Admin admin;
        try {
            admin = adminService.dangNhap(username, matKhau);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() != HttpStatus.UNAUTHORIZED.value()) throw e;
            int conLai = loginAttempts.dangNhapSai(username, ip);
            AuditLog.warn("LOGIN_FAILED", "user=" + username + " ip=" + ip + " conLai=" + conLai);
            if (conLai == 0) {
                AuditLog.warn("LOGIN_LOCKED", "user=" + username + " ip=" + ip);
                loginAttempts.kiemTraKhoa(username, ip); // ném 429 kèm thời gian khoá
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Sai tài khoản hoặc mật khẩu! (còn " + conLai + " lần thử)");
        }

        loginAttempts.dangNhapDung(username, ip);
        AuditLog.info("LOGIN_OK", "user=" + admin.getUsername() + " ip=" + ip);
        Map<String, Object> res = thongTin(admin);
        res.put("message", "Đăng nhập thành công!");
        res.put("token", admin.getToken());
        res.put("hetHan", admin.getTokenHetHan().toString());
        return res;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        Admin admin = authService.yeuCauNhanVien(request);
        adminService.dangXuat(admin);
        AuditLog.info("LOGOUT", "user=" + admin.getUsername() + " ip=" + ClientIp.of(request));
        return Map.of("message", "Đăng xuất thành công!");
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        return thongTin(authService.yeuCauNhanVien(request));
    }

    private static Map<String, Object> thongTin(Admin admin) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", admin.getId());
        m.put("idAdmin", admin.getIdAdmin());
        m.put("username", admin.getUsername());
        return m;
    }
}
