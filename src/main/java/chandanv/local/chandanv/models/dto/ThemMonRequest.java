package chandanv.local.chandanv.models.dto;

import java.util.ArrayList;
import java.util.List;

/** Yêu cầu thêm 1 món vào giỏ / đơn tại quầy. Client chỉ gửi mã tuỳ chọn, server tự tính giá. */
public class ThemMonRequest {

    private Integer idMon;
    private Integer soLuong = 1;
    private String size = "M";
    private List<String> toppings = new ArrayList<>();
    private String doNgot = "100%";
    private String ghiChu;

    public Integer getIdMon() { return idMon; }
    public void setIdMon(Integer idMon) { this.idMon = idMon; }

    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public List<String> getToppings() { return toppings; }
    public void setToppings(List<String> toppings) { this.toppings = toppings != null ? toppings : new ArrayList<>(); }

    public String getDoNgot() { return doNgot; }
    public void setDoNgot(String doNgot) { this.doNgot = doNgot; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
