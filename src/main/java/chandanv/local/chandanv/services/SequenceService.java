package chandanv.local.chandanv.services;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import chandanv.local.chandanv.models.entity.Counter;

/** Sinh mã tăng dần, nguyên tử (an toàn khi nhiều request cùng lúc). */
@Service
public class SequenceService {

    private final MongoTemplate mongo;

    public SequenceService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public long next(String name) {
        Counter c = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(name)),
                new Update().inc("seq", 1L),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Counter.class);
        return c != null ? c.getSeq() : 1L;
    }

    /** Đảm bảo bộ đếm không nhỏ hơn giá trị cho trước (dùng khi đã có dữ liệu cũ). */
    public void ensureAtLeast(String name, long value) {
        mongo.upsert(Query.query(Criteria.where("_id").is(name)), new Update().max("seq", value), Counter.class);
    }
}
