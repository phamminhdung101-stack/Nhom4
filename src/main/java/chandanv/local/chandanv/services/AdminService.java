package chandanv.local.chandanv.services;

import chandanv.local.chandanv.models.entity.Admin;

public interface AdminService {

    Admin login(String username, String matKhau);

    Admin save(Admin admin);

    Admin findById(String id);
}