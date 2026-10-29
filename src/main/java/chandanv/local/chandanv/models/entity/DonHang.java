package chandanv.local.chandanv.models.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "don_hang")
public class DonHang {

    // Trạng thái đơn
    public static final int CHO_XAC_NHAN = 0;
    public static final int DANG_PHA_CHE = 1;
    public static final int DANG_MANG_RA = 2;
    public static final int HOAN_THANH = 3;
    /** Đơn bị huỷ (khách tự huỷ khi còn chờ xác nhận, hoặc nhân viên huỷ) – không tính doanh thu. */
    public static final int DA_HUY = 4;

    public static final String LOAI_TAI_BAN = "TAI_BAN";
    public static final String LOAI_TAI_QUAY = "TAI_QUAY";

    @Id
    private String id;

    /** Mã đơn tăng dần (lấy từ collection counters), dùng để hiển thị #DH-xx. */
    @Indexed(unique = true)
    private Long idDonHang;

    /** Bàn đặt món; 0 = khách mua tại quầy. */
    @Indexed
    private Integer idBan;

    private String loai = LOAI_TAI_BAN;

    private Integer idKhuyenMai;

    private String ghiChu;

    private List<MonDaChon> items = new ArrayList<>();

    private Long tongTien;

    /** Có index: màn hình bếp / đếm đơn đang chờ lọc theo trạng thái thường xuyên. */
    @Indexed
    private Integer trangThai = CHO_XAC_NHAN;

    @Indexed
    private Instant thoiGian;

    private Instant thoiGianCapNhat;

    /** Máy nào trong bàn đặt đơn ("Máy 2"...), hoặc "Nhân viên". */
    private String nguoiDat;

    private String lyDoHuy;

    /** Ai huỷ đơn: tên đăng nhập nhân viên, hoặc "Khách (Bàn n)". */
    private String nguoiHuy;

    public DonHang() {
        this.thoiGian = Instant.now();
        this.thoiGianCapNhat = this.thoiGian;
    }

    public static boolean trangThaiHopLe(Integer t) {
        return t != null && t >= CHO_XAC_NHAN && t <= HOAN_THANH;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getIdDonHang() { return idDonHang; }
    public void setIdDonHang(Long idDonHang) { this.idDonHang = idDonHang; }

    public Integer getIdBan() { return idBan; }
    public void setIdBan(Integer idBan) { this.idBan = idBan; }

    public String getLoai() { return loai; }
    public void setLoai(String loai) { this.loai = loai; }

    public Integer getIdKhuyenMai() { return idKhuyenMai; }
    public void setIdKhuyenMai(Integer idKhuyenMai) { this.idKhuyenMai = idKhuyenMai; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public List<MonDaChon> getItems() {
        if (items == null) items = new ArrayList<>();
        return items;
    }
    public void setItems(List<MonDaChon> items) { this.items = items != null ? items : new ArrayList<>(); }

    public Long getTongTien() { return tongTien; }
    public void setTongTien(Long tongTien) { this.tongTien = tongTien; }

    public Integer getTrangThai() { return trangThai; }
    public void setTrangThai(Integer trangThai) { this.trangThai = trangThai; }

    public Instant getThoiGian() { return thoiGian; }
    public void setThoiGian(Instant thoiGian) { this.thoiGian = thoiGian; }

    public Instant getThoiGianCapNhat() { return thoiGianCapNhat; }
    public void setThoiGianCapNhat(Instant thoiGianCapNhat) { this.thoiGianCapNhat = thoiGianCapNhat; }

    public String getNguoiDat() { return nguoiDat; }
    public void setNguoiDat(String nguoiDat) { this.nguoiDat = nguoiDat; }

    public String getLyDoHuy() { return lyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }

    public String getNguoiHuy() { return nguoiHuy; }
    public void setNguoiHuy(String nguoiHuy) { this.nguoiHuy = nguoiHuy; }
}
