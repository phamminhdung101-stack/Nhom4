package chandanv.local.chandanv.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Máy chưa cài / chưa bật MongoDB -> thay stacktrace dài bằng khung "APPLICATION FAILED TO START"
 * có hướng dẫn khắc phục (đăng ký trong META-INF/spring.factories).
 */
public class MongoFailureAnalyzer extends AbstractFailureAnalyzer<DataAccessResourceFailureException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, DataAccessResourceFailureException cause) {
        String uri = System.getenv().getOrDefault("MONGODB_URI", "mongodb://localhost:27017/tiemtra");
        return new FailureAnalysis(
                "Không kết nối được MongoDB tại " + uri + " (" + cause.getMostSpecificCause().getMessage() + ")",
                """
                1. Cài MongoDB Community Server: https://www.mongodb.com/try/download/community
                2. Bật dịch vụ MongoDB (Windows: Win+R -> services.msc -> MongoDB Server -> Start)
                3. Chạy lại: .\\mvnw.cmd spring-boot:run
                   Dùng MongoDB ở máy / địa chỉ khác: đặt biến môi trường MONGODB_URI=mongodb://<máy>:27017/tiemtra
                (Dữ liệu mẫu – tài khoản, thực đơn – được tự nạp khi DB còn trống, không cần mongoimport.)""",
                cause);
    }
}
