package chandanv.local.chandanv.models.entity;

import java.time.Instant;

public class GioHangItem {

    private String id;

    private Integer idGioHang;
    private Integer idBan;
    private Integer idMon;

    private String tenMon;

    private Integer trangThai;
    private Instant thoiGianTao;
    private Integer soLuong;
    private Double donGia;
    private Double thanhTien;
    private String ghiChu;
    private Double tongTienTamTinh;

    public GioHangItem() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getIdGioHang() {
        return idGioHang;
    }

    public void setIdGioHang(Integer idGioHang) {
        this.idGioHang = idGioHang;
    }

    public Integer getIdBan() {
        return idBan;
    }

    public void setIdBan(Integer idBan) {
        this.idBan = idBan;
    }

    public Integer getIdMon() {
        return idMon;
    }

    public void setIdMon(Integer idMon) {
        this.idMon = idMon;
    }

    public String getTenMon() {
        return tenMon;
    }

    public void setTenMon(String tenMon) {
        this.tenMon = tenMon;
    }

    public Integer getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Integer trangThai) {
        this.trangThai = trangThai;
    }

    public Instant getThoiGianTao() {
        return thoiGianTao;
    }

    public void setThoiGianTao(Instant thoiGianTao) {
        this.thoiGianTao = thoiGianTao;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public Double getDonGia() {
        return donGia;
    }

    public void setDonGia(Double donGia) {
        this.donGia = donGia;
    }

    public Double getThanhTien() {
        return thanhTien;
    }

    public void setThanhTien(Double thanhTien) {
        this.thanhTien = thanhTien;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public Double getTongTienTamTinh() {
        return tongTienTamTinh;
    }

    public void setTongTienTamTinh(Double tongTienTamTinh) {
        this.tongTienTamTinh = tongTienTamTinh;
    }
}