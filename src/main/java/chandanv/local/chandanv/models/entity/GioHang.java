package chandanv.local.chandanv.models.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "gio_hang")
public class GioHang {

    @Id
    private String id;

    @Indexed(unique = true)
    private Integer idBan;

    private Instant createdAt;
    private Instant updatedAt;

    private List<MonDaChon> items = new ArrayList<>();

    /** Tăng mỗi lần ghi – để phát hiện 2 điện thoại cùng bàn sửa giỏ cùng lúc (xem GioHangService.suaGio). */
    private Long phienBan;

    public GioHang() {}

    public GioHang(Integer idBan) {
        this.idBan = idBan;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }

    public Integer getIdBan() { return idBan; }
    public void setIdBan(Integer idBan) { this.idBan = idBan; }

    public List<MonDaChon> getItems() {
        if (items == null) items = new ArrayList<>();
        return items;
    }

    public void setItems(List<MonDaChon> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public Long getPhienBan() { return phienBan; }
    public void setPhienBan(Long phienBan) { this.phienBan = phienBan; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /** Tổng tiền tạm tính (tính lại mỗi lần đọc, không lưu). */
    public long getTongTien() {
        return getItems().stream().mapToLong(MonDaChon::getThanhTien).sum();
    }

    public int getTongSoLuong() {
        return getItems().stream().mapToInt(MonDaChon::getSoLuong).sum();
    }
}
