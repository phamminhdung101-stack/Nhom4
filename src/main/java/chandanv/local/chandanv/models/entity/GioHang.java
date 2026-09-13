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

    private List<GioHangItem> items = new ArrayList<>();

    public GioHang() {}

    public GioHang(Integer idBan, List<GioHangItem> items) {
        this.idBan = idBan;
        this.items = items != null ? items : new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Integer getIdBan() {
        return idBan;
    }

    public void setIdBan(Integer idBan) {
        this.idBan = idBan;
    }

    public List<GioHangItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<GioHangItem> items) {
        this.items = (items != null) ? items : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}