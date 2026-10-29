package chandanv.local.chandanv.models.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Một dòng món đã chọn (dùng chung cho giỏ hàng và đơn hàng).
 * Giá luôn do server tính từ bảng mon_an + bảng tuỳ chọn, không tin giá client gửi lên.
 */
public class MonDaChon {

    private String id;          // id dòng (UUID) – dùng để sửa/xoá đúng dòng
    private int idMon;
    private String tenMon;
    private String size;        // "M" | "L"
    private List<String> toppings = new ArrayList<>(); // mã topping: t1, t2...
    private String doNgot;      // "100%", "70%"...
    private String moTaTuyChon; // chuỗi hiển thị: "Size L · 70% đường · Trân châu đen"
    private String ghiChu;
    private int soLuong;
    private long donGia;
    private long thanhTien;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getIdMon() { return idMon; }
    public void setIdMon(int idMon) { this.idMon = idMon; }

    public String getTenMon() { return tenMon; }
    public void setTenMon(String tenMon) { this.tenMon = tenMon; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public List<String> getToppings() { return toppings; }
    public void setToppings(List<String> toppings) {
        this.toppings = toppings != null ? toppings : new ArrayList<>();
    }

    public String getDoNgot() { return doNgot; }
    public void setDoNgot(String doNgot) { this.doNgot = doNgot; }

    public String getMoTaTuyChon() { return moTaTuyChon; }
    public void setMoTaTuyChon(String moTaTuyChon) { this.moTaTuyChon = moTaTuyChon; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public long getDonGia() { return donGia; }
    public void setDonGia(long donGia) { this.donGia = donGia; }

    public long getThanhTien() { return thanhTien; }
    public void setThanhTien(long thanhTien) { this.thanhTien = thanhTien; }

    /** Hai dòng được gộp khi cùng món và cùng toàn bộ tuỳ chọn. */
    public boolean cungTuyChon(MonDaChon other) {
        return other != null
                && idMon == other.idMon
                && java.util.Objects.equals(size, other.size)
                && java.util.Objects.equals(doNgot, other.doNgot)
                && java.util.Objects.equals(ghiChu, other.ghiChu)
                && new java.util.TreeSet<>(toppings).equals(new java.util.TreeSet<>(other.toppings));
    }

    public void tinhThanhTien() {
        this.thanhTien = donGia * soLuong;
    }
}
