package chandanv.local.chandanv.models.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lich_su_ban_hang")
public class LichSuBanHang {

    @Id
    private String _id;

    private int idLichSu;
    private int idChiTiet;
    private int tongTienGoc;
    private int tienGiam;
    private int tongTienThanhToan;

    public int getIdLichSu() { return idLichSu; }
    public void setIdLichSu(int idLichSu) { this.idLichSu = idLichSu; }

    public int getIdChiTiet() { return idChiTiet; }
    public void setIdChiTiet(int idChiTiet) { this.idChiTiet = idChiTiet; }

    public int getTongTienGoc() { return tongTienGoc; }
    public void setTongTienGoc(int tongTienGoc) { this.tongTienGoc = tongTienGoc; }

    public int getTienGiam() { return tienGiam; }
    public void setTienGiam(int tienGiam) { this.tienGiam = tienGiam; }

    public int getTongTienThanhToan() { return tongTienThanhToan; }
    public void setTongTienThanhToan(int tongTienThanhToan) { this.tongTienThanhToan = tongTienThanhToan; }
}
