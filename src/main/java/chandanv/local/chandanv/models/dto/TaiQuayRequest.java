package chandanv.local.chandanv.models.dto;

import java.util.ArrayList;
import java.util.List;

/** Đơn tạo tại quầy thu ngân (POS). */
public class TaiQuayRequest {

    private List<ThemMonRequest> items = new ArrayList<>();
    private String ghiChu;

    public List<ThemMonRequest> getItems() { return items; }
    public void setItems(List<ThemMonRequest> items) { this.items = items != null ? items : new ArrayList<>(); }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
