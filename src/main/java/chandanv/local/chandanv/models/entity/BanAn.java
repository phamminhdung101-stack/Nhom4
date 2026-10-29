package chandanv.local.chandanv.models.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "ban_an")
public class BanAn {

    public static final int TRONG = 0;
    public static final int CO_KHACH = 1;

    @Id
    private String id;

    @Indexed(unique = true)
    private int idBan;

    private String tenBan;

    /** Đường dẫn tương đối mà mã QR trỏ tới, ví dụ /welcome.html?idBan=1&t=xxxx */
    private String maQR;

    /**
     * Mã bí mật in trong QR. Chỉ dùng để XIN VÀO bàn (POST /vao-ban), không đặt món trực tiếp được:
     * sau khi vào, mỗi điện thoại dùng mã riêng của mình (ThietBi.token). Đổi mã: nút "Đổi mã" ở trang Mã QR.
     */
    private String token;

    /** Ảnh QR (PNG, base64 không có tiền tố data:) – không trả về trong danh sách để giảm dung lượng. */
    @JsonIgnore
    private String qrImageBase64;

    private String thoiGianDatBan;

    /** 0 = trống, 1 = có khách */
    private int trangThai;

    private String ghiChu;

    /** Các điện thoại trong phiên bàn hiện tại (xoá khi bàn về Trống). */
    private List<ThietBi> thietBi = new ArrayList<>();

    /** Đếm số máy đã vào trong phiên (đặt tên "Máy n"). */
    private int soThietBi;

    // Phiên vừa kết thúc: máy của phiên đó vẫn XEM LẠI được đơn của mình (chỉ xem, không đặt món)
    @JsonIgnore
    private List<String> phienCuTokens = new ArrayList<>();
    private String phienCuBatDau;
    private String phienCuKetThuc;

    public BanAn() {}

    public String getId() { return id; }

    public int getIdBan() { return idBan; }
    public void setIdBan(int idBan) { this.idBan = idBan; }

    public String getTenBan() { return tenBan; }
    public void setTenBan(String tenBan) { this.tenBan = tenBan; }

    public String getMaQR() { return maQR; }
    public void setMaQR(String maQR) { this.maQR = maQR; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getQrImageBase64() { return qrImageBase64; }
    public void setQrImageBase64(String qrImageBase64) { this.qrImageBase64 = qrImageBase64; }

    public boolean isCoAnhQR() { return qrImageBase64 != null && !qrImageBase64.isBlank(); }

    public String getThoiGianDatBan() { return thoiGianDatBan; }
    public void setThoiGianDatBan(String thoiGianDatBan) { this.thoiGianDatBan = thoiGianDatBan; }

    public int getTrangThai() { return trangThai; }
    public void setTrangThai(int trangThai) {
        if (trangThai != TRONG && trangThai != CO_KHACH) {
            throw new IllegalArgumentException("Trạng thái bàn không hợp lệ (chỉ 0 hoặc 1)");
        }
        this.trangThai = trangThai;
    }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public List<ThietBi> getThietBi() {
        if (thietBi == null) thietBi = new ArrayList<>();
        return thietBi;
    }
    public void setThietBi(List<ThietBi> thietBi) { this.thietBi = thietBi != null ? thietBi : new ArrayList<>(); }

    public int getSoThietBi() { return soThietBi; }
    public void setSoThietBi(int soThietBi) { this.soThietBi = soThietBi; }

    public List<String> getPhienCuTokens() {
        if (phienCuTokens == null) phienCuTokens = new ArrayList<>();
        return phienCuTokens;
    }
    public void setPhienCuTokens(List<String> phienCuTokens) { this.phienCuTokens = phienCuTokens; }

    public String getPhienCuBatDau() { return phienCuBatDau; }
    public void setPhienCuBatDau(String phienCuBatDau) { this.phienCuBatDau = phienCuBatDau; }

    public String getPhienCuKetThuc() { return phienCuKetThuc; }
    public void setPhienCuKetThuc(String phienCuKetThuc) { this.phienCuKetThuc = phienCuKetThuc; }
}
