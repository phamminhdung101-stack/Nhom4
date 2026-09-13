package chandanv.local.chandanv.models.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "don_hang")
public class DonHang {

    @Id
    private String id;

    @Field("idDonHang")
    private Integer idDonHang;

    @Field("idGioHang")
    private Integer idGioHang;

    @Field("idKhuyenMai")
    private Integer idKhuyenMai;

    @Field("ghiChu")
    private String ghiChu;

    @Field("tongTien")
    private Double tongTien;

    @Field("trangThai")
    private Integer trangThai;

    @Field("thoiGian")
    private Instant thoiGian;

    public DonHang() {
        this.thoiGian = Instant.now();
        this.trangThai = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getIdDonHang() { return idDonHang; }
    public void setIdDonHang(Integer idDonHang) { this.idDonHang = idDonHang; }

    public Integer getIdGioHang() { return idGioHang; }
    public void setIdGioHang(Integer idGioHang) { this.idGioHang = idGioHang; }

    public Integer getIdKhuyenMai() {
         return idKhuyenMai; 
        }
    public void setIdKhuyenMai(Integer idKhuyenMai) {
         this.idKhuyenMai = idKhuyenMai; 
        }

    public String getGhiChu() {
         return ghiChu; 
        }
    public void setGhiChu(String ghiChu) {
         this.ghiChu = ghiChu; 
        }

    public Double getTongTien() {
         return tongTien; 
        }
    public void setTongTien(Double tongTien) {
         this.tongTien = tongTien; 
        }

    public Integer getTrangThai() {
         return trangThai; 
        }
    public void setTrangThai(Integer trangThai) {
         this.trangThai = trangThai; 
        }

    public Instant getThoiGian() {
         return thoiGian; 
        }
    public void setThoiGian(Instant thoiGian) {
         this.thoiGian = thoiGian; 
        }
}