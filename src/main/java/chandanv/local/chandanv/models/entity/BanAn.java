package chandanv.local.chandanv.models.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ban_an")
public class BanAn {

    @Id
    private String id;

    private int idBan;
    private String tenBan;

    // Nội dung QR (link)
    private String maQR;

    // Ảnh QR dạng Base64
    private String qrImageBase64;

    private String thoiGianDatBan;

    // 0 = trống, 1 = có khách
    private int trangThai;

    private String ghiChu;

    public BanAn() {}

    public String getId() {
        return id;
    }

    public int getIdBan() {
        return idBan;
    }

    public void setIdBan(int idBan) {
        this.idBan = idBan;
    }

    public String getTenBan() {
        return tenBan;
    }

    public void setTenBan(String tenBan) {
        this.tenBan = tenBan;
    }

    public String getMaQR() {
        return maQR;
    }

    public void setMaQR(String maQR) {
        this.maQR = maQR;
    }

    public String getQrImageBase64() {
        return qrImageBase64;
    }

    public void setQrImageBase64(String qrImageBase64) {
        this.qrImageBase64 = qrImageBase64;
    }

    public String getThoiGianDatBan() {
        return thoiGianDatBan;
    }

    public void setThoiGianDatBan(String thoiGianDatBan) {
        this.thoiGianDatBan = thoiGianDatBan;
    }

    public int getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(int trangThai) {
        if (trangThai != 0 && trangThai != 1) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ (chỉ 0 hoặc 1)");
        }
        this.trangThai = trangThai;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }
}