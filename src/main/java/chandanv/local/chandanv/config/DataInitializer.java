package chandanv.local.chandanv.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.models.entity.DonHang;
import chandanv.local.chandanv.repositories.BanAnRepository;
import chandanv.local.chandanv.repositories.DonHangRepository;
import chandanv.local.chandanv.services.AdminService;
import chandanv.local.chandanv.services.BanAnService;
import chandanv.local.chandanv.services.DonHangService;
import chandanv.local.chandanv.services.SequenceService;

/**
 * Chạy 1 lần khi khởi động:
 *  0. DB trống -> tự nạp dữ liệu mẫu từ db_tiemtra/
 *     (tài khoản, thực đơn...) để máy mới clone project về chạy được ngay, không cần mongoimport.
 *  1. Tạo 10 bàn nếu DB trống; bổ sung mã QR/token cho bàn cũ (sửa dữ liệu maQR bị ghi đè).
 *  2. Băm BCrypt mật khẩu admin còn lưu dạng chữ thường.
 *  3. Chuyển đơn cũ (chỉ có idGioHang) sang trường idBan.
 *  4. Đặt bộ đếm mã đơn >= mã đơn lớn nhất hiện có.
 *  5. Tạo thư mục ảnh upload nếu chưa có.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BanAnRepository banAnRepository;
    private final DonHangRepository donHangRepository;
    private final AdminService adminService;
    private final SequenceService sequenceService;
    private final MongoTemplate mongoTemplate;
    private final String uploadDir;
    private final String seedDir;

    /**
     * Dữ liệu mẫu tự nạp khi collection còn trống. Không nạp ban_an (bàn được tạo mới với mã QR ngẫu nhiên riêng
     * cho từng máy, không dùng mã công khai trong repo) và không nạp đơn hàng / giỏ hàng thử nghiệm.
     */
    private static final List<String> DU_LIEU_MAU = List.of("admin", "loai_mon_an", "mon_an", "khuyen_mai", "nhan_vien");

    public DataInitializer(BanAnRepository banAnRepository,
                           DonHangRepository donHangRepository,
                           AdminService adminService,
                           SequenceService sequenceService,
                           MongoTemplate mongoTemplate,
                           @Value("${app.upload-dir:./uploads}") String uploadDir,
                           @Value("${app.seed-dir:./db_tiemtra}") String seedDir) {
        this.banAnRepository = banAnRepository;
        this.donHangRepository = donHangRepository;
        this.adminService = adminService;
        this.sequenceService = sequenceService;
        this.mongoTemplate = mongoTemplate;
        this.uploadDir = uploadDir;
        this.seedDir = seedDir;
    }

    @Override
    public void run(String... args) throws Exception {
        napDuLieuMau();

        if (banAnRepository.count() == 0) {
            for (int i = 1; i <= 10; i++) {
                BanAn ban = new BanAn();
                ban.setIdBan(i);
                ban.setTenBan(BanAnService.tenBanMacDinh(i));
                ban.setTrangThai(BanAn.TRONG);
                BanAnService.damBaoMaQR(ban);
                banAnRepository.save(ban);
            }
            log.info("Đã tạo sẵn 10 bàn (1–10)");
        }

        int banDaSua = 0;
        for (BanAn ban : banAnRepository.findAll()) {
            if (BanAnService.damBaoMaQR(ban)) {
                banAnRepository.save(ban);
                banDaSua++;
            }
        }
        if (banDaSua > 0) log.info("Đã cấp/sửa mã QR cho {} bàn", banDaSua);

        int daBam = adminService.bamMatKhauCu();
        if (daBam > 0) log.info("Đã băm BCrypt mật khẩu cho {} tài khoản cũ", daBam);

        Query donCu = new Query(Criteria.where("idBan").exists(false).and("idGioHang").exists(true));
        int donDaChuyen = 0;
        for (Document doc : mongoTemplate.find(donCu, Document.class, "don_hang")) {
            Object idGioHang = doc.get("idGioHang");
            int idBan = idGioHang instanceof Number num ? num.intValue() : 0;
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(doc.get("_id"))),
                    Update.update("idBan", idBan), "don_hang");
            donDaChuyen++;
        }
        if (donDaChuyen > 0) log.info("Đã chuyển {} đơn cũ sang trường idBan", donDaChuyen);

        long maxId = donHangRepository.findFirstByOrderByIdDonHangDesc()
                .map(DonHang::getIdDonHang).orElse(0L);
        sequenceService.ensureAtLeast(DonHangService.COUNTER_DON_HANG, maxId);

        Files.createDirectories(Paths.get(uploadDir));
    }

    /** Collection còn trống + có file mẫu db_tiemtra/tiemtra.<tên>.json -> nạp vào (giống mongoimport). */
    private void napDuLieuMau() {
        for (String ten : DU_LIEU_MAU) {
            Path file = Paths.get(seedDir, "tiemtra." + ten + ".json");
            if (!Files.exists(file) || mongoTemplate.getCollection(ten).countDocuments() > 0) continue;
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                // File là mảng JSON xuất từ mongoexport ({"$oid": ...}) -> bọc vào 1 document để parse
                List<Document> docs = Document.parse("{\"d\": " + json + "}").getList("d", Document.class);
                if (!docs.isEmpty()) {
                    mongoTemplate.getCollection(ten).insertMany(docs);
                    log.info("Đã nạp {} bản ghi mẫu vào '{}' từ {}", docs.size(), ten, file);
                }
            } catch (Exception e) {
                log.warn("Không nạp được dữ liệu mẫu {}: {}", file, e.getMessage());
            }
        }
    }
}
