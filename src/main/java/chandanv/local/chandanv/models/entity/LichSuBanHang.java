package chandanv.local.chandanv.models.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Ghi lại mỗi đơn khi chuyển sang Hoàn thành (giữ cấu trúc collection lich_su_ban_hang của dữ liệu cũ).
 * Trang Doanh thu KHÔNG đọc bảng này: báo cáo tính trực tiếp từ don_hang (DonHangService.baoCao).
 */
@Document(collection = "lich_su_ban_hang")
public class LichSuBanHang {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private Long idDonHang;

    private Integer idBan;
    private long tongTienGoc;
    private long tienGiam;
    private long tongTienThanhToan;
    private Instant thoiGian;

    public String getId() { return id; }

    public Long getIdDonHang() { return idDonHang; }
    public void setIdDonHang(Long idDonHang) { this.idDonHang = idDonHang; }

    public Integer getIdBan() { return idBan; }
    public void setIdBan(Integer idBan) { this.idBan = idBan; }

    public long getTongTienGoc() { return tongTienGoc; }
    public void setTongTienGoc(long tongTienGoc) { this.tongTienGoc = tongTienGoc; }

    public long getTienGiam() { return tienGiam; }
    public void setTienGiam(long tienGiam) { this.tienGiam = tienGiam; }

    public long getTongTienThanhToan() { return tongTienThanhToan; }
    public void setTongTienThanhToan(long tongTienThanhToan) { this.tongTienThanhToan = tongTienThanhToan; }

    public Instant getThoiGian() { return thoiGian; }
    public void setThoiGian(Instant thoiGian) { this.thoiGian = thoiGian; }
}
