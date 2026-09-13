package chandanv.local.chandanv.models.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chi_tiet_don")
public class ChiTietDonHang {

    @Id
    private String _id;

    private int idChiTiet;
    private int idDonHang;
    private int idNV;
    private int soLuong;
    private DonGiaThanhTien donGiaThanhTien;
    private String ghiChu;
    private int trangThai;

    /*  GET / SET  */

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public int getIdChiTiet() {
        return idChiTiet;
    }

    public void setIdChiTiet(int idChiTiet) {
        this.idChiTiet = idChiTiet;
    }

    public int getIdDonHang() {
        return idDonHang;
    }

    public void setIdDonHang(int idDonHang) {
        this.idDonHang = idDonHang;
    }

    public int getIdNV() {
        return idNV;
    }

    public void setIdNV(int idNV) {
        this.idNV = idNV;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public DonGiaThanhTien getDonGiaThanhTien() {
        return donGiaThanhTien;
    }

    public void setDonGiaThanhTien(DonGiaThanhTien donGiaThanhTien) {
        this.donGiaThanhTien = donGiaThanhTien;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public int getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(int trangThai) {
        this.trangThai = trangThai;
    }

  
    public static class DonGiaThanhTien {
        private int donGia;
        private int thanhTien;

        public int getDonGia() {
            return donGia;
        }

        public void setDonGia(int donGia) {
            this.donGia = donGia;
        }

        public int getThanhTien() {
            return thanhTien;
        }

        public void setThanhTien(int thanhTien) {
            this.thanhTien = thanhTien;
        }
    }
}
