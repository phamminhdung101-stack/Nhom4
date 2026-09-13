package chandanv.local.chandanv.models.entity;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "admin")
public class Admin {

    @Id
    private String id; 

    private String idAdmin;
    private String username;
    private String matKhau;
    private Date thoiGianDangNhap;
    private Date thoiGianDangXuat;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdAdmin() {
        return idAdmin;
    }

    public void setIdAdmin(String idAdmin) {
        this.idAdmin = idAdmin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }

    public Date getThoiGianDangNhap() {
        return thoiGianDangNhap;
    }

    public void setThoiGianDangNhap(Date thoiGianDangNhap) {
        this.thoiGianDangNhap = thoiGianDangNhap;
    }

    public Date getThoiGianDangXuat() {
        return thoiGianDangXuat;
    }

    public void setThoiGianDangXuat(Date thoiGianDangXuat) {
        this.thoiGianDangXuat = thoiGianDangXuat;
    }
}
