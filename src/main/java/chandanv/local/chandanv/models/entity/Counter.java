package chandanv.local.chandanv.models.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Bộ đếm tăng dần nguyên tử (findAndModify $inc) để sinh mã đơn #DH-1, #DH-2... không trùng khi nhiều đơn tạo cùng lúc. */
@Document(collection = "counters")
public class Counter {

    @Id
    private String id;

    private long seq;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getSeq() { return seq; }
    public void setSeq(long seq) { this.seq = seq; }
}
