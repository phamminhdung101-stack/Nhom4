package chandanv.local.chandanv.models.entity;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Một điện thoại đang ngồi ở bàn (lưu kèm trong BanAn, mất đi khi bàn trả về Trống).
 * Máy đầu tiên quét QR bàn trống = chủ bàn (được duyệt ngay); máy sau phải chờ nhân viên duyệt
 * -> người lạ quét trộm QR không đặt món được vào bàn người khác.
 */
public class ThietBi {

    public static final String CHO_DUYET = "CHO_DUYET";
    public static final String DA_DUYET = "DA_DUYET";

    /** Mã công khai – nhân viên dùng để duyệt / mời ra. */
    private String id;

    /** Mã bí mật chỉ máy đó biết (header X-Table-Token). Không trả ra JSON. */
    @JsonIgnore
    private String token;

    /** "Máy 1", "Máy 2"... */
    private String ten;

    private boolean laChu;

    private String trangThai = CHO_DUYET;

    private Instant vaoLuc;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }

    public boolean isLaChu() { return laChu; }
    public void setLaChu(boolean laChu) { this.laChu = laChu; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public Instant getVaoLuc() { return vaoLuc; }
    public void setVaoLuc(Instant vaoLuc) { this.vaoLuc = vaoLuc; }

    @JsonIgnore
    public boolean isDaDuyet() { return DA_DUYET.equals(trangThai); }
}
