// AdminController.java
package chandanv.local.chandanv.controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.services.AdminService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,
                                   @RequestParam String matKhau) {

        Admin admin = adminService.login(username, matKhau);

        if (admin != null) {

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đăng nhập thành công!");
            response.put("id", admin.getId());
            response.put("idAdmin", admin.getIdAdmin());
            response.put("username", admin.getUsername());

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest()
                .body("Sai tài khoản hoặc mật khẩu!");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String id) {

        Admin admin = adminService.findById(id);

        if (admin != null) {
            admin.setThoiGianDangXuat(new Date());
            adminService.save(admin);
            return ResponseEntity.ok("Đăng xuất thành công!");
        }

        return ResponseEntity.badRequest()
                .body("Admin không tồn tại!");
    }
}