package chandanv.local.chandanv.services.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.repositories.AdminRepository;
import chandanv.local.chandanv.services.AdminService;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public Admin login(String username, String matKhau) {

        Optional<Admin> optionalAdmin = adminRepository.findByUsername(username);

        if (optionalAdmin.isPresent()) {
            Admin admin = optionalAdmin.get();

            if (admin.getMatKhau().equals(matKhau)) {
                admin.setThoiGianDangNhap(new Date());
                return adminRepository.save(admin);
            }
        }

        return null;
    }

    @Override
    public Admin save(Admin admin) {
        return adminRepository.save(admin);
    }

    @Override
    public Admin findById(String id) {
        return adminRepository.findById(id).orElse(null);
    }
}