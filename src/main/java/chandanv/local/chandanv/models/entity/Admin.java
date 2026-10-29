package chandanv.local.chandanv.models.entity;

import java.time.Instant;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "admin")
public class Admin {

    @Id
    private String id;

    private String idAdmin;

    @Indexed(unique = true)
    private String username;

    /** Mật khẩu đã băm BCrypt (dữ liệu cũ dạng chữ thường sẽ tự được băm khi khởi động). */
    @JsonIgnore
    private String matKhau;

    /** Token phiên đăng nhập, gửi kèm header X-Admin-Token. */
    @JsonIgnore
    @Indexed(sparse = true)
    private String token;

    @JsonIgnore
    private Instant tokenHetHan;

    private Date thoiGianDangNhap;
    private Date thoiGianDangXuat;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdAdmin() { return idAdmin; }
    public void setIdAdmin(String idAdmin) { this.idAdmin = idAdmin; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Instant getTokenHetHan() { return tokenHetHan; }
    public void setTokenHetHan(Instant tokenHetHan) { this.tokenHetHan = tokenHetHan; }

    public Date getThoiGianDangNhap() { return thoiGianDangNhap; }
    public void setThoiGianDangNhap(Date thoiGianDangNhap) { this.thoiGianDangNhap = thoiGianDangNhap; }

    public Date getThoiGianDangXuat() { return thoiGianDangXuat; }
    public void setThoiGianDangXuat(Date thoiGianDangXuat) { this.thoiGianDangXuat = thoiGianDangXuat; }
}
