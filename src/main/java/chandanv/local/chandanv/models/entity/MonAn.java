package chandanv.local.chandanv.models.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "mon_an")
public class MonAn {

    @Id
    private String id;

    private int idMon;

    // Dữ liệu trong MongoDB đang dùng tên trường viết hoa "TenMon"/"Gia" -> giữ nguyên tên cột,
    // còn trong Java/JSON dùng camelCase chuẩn (tenMon, gia).
    @Field("TenMon")
    private String tenMon;

    private int idLoai;

    @Field("Gia")
    private int gia;

    /** Đường dẫn ảnh, ví dụ /uploads/1.jpg */
    private String image;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getIdMon() { return idMon; }
    public void setIdMon(int idMon) { this.idMon = idMon; }

    public String getTenMon() { return tenMon; }
    public void setTenMon(String tenMon) { this.tenMon = tenMon; }

    public int getIdLoai() { return idLoai; }
    public void setIdLoai(int idLoai) { this.idLoai = idLoai; }

    public int getGia() { return gia; }
    public void setGia(int gia) { this.gia = gia; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
