package chandanv.local.chandanv.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import chandanv.local.chandanv.models.dto.ThemMonRequest;
import chandanv.local.chandanv.models.entity.MonAn;
import chandanv.local.chandanv.models.entity.MonDaChon;
import chandanv.local.chandanv.repositories.MonAnRepository;
import chandanv.local.chandanv.util.TextUtil;

@Service
public class MonAnService {

    /** Đuôi ảnh được dò khi đường dẫn trong DB không trỏ tới file có thật. */
    private static final List<String> DUOI_ANH = List.of(".jpg", ".jpeg", ".png", ".webp");

    private final MonAnRepository repository;
    private final Path thuMucAnh;

    public MonAnService(MonAnRepository repository, @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.repository = repository;
        this.thuMucAnh = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public List<MonAn> getAll() {
        return repository.findAllByOrderByIdLoaiAscIdMonAsc().stream().map(this::chuanHoaAnh).toList();
    }

    public List<MonAn> getByIdLoai(int idLoai) {
        return repository.findByIdLoaiOrderByIdMonAsc(idLoai).stream().map(this::chuanHoaAnh).toList();
    }

    public MonAn requireMon(int idMon) {
        return repository.findFirstByIdMon(idMon)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món #" + idMon));
    }

    /**
     * Tạo 1 dòng món đã định giá từ yêu cầu của client.
     * Giá = giá món trong DB + giá size + giá topping (bảng TuyChonMon). Mọi giá client gửi lên đều bị bỏ qua.
     */
    public MonDaChon taoDong(ThemMonRequest req) {
        if (req == null || req.getIdMon() == null) {
            throw badRequest("Thiếu mã món");
        }
        int soLuong = req.getSoLuong() == null ? 1 : req.getSoLuong();
        if (soLuong < 1 || soLuong > TuyChonMon.SO_LUONG_TOI_DA) {
            throw badRequest("Số lượng phải từ 1 đến " + TuyChonMon.SO_LUONG_TOI_DA);
        }

        MonAn mon = requireMon(req.getIdMon());

        TuyChonMon.Size size = TuyChonMon.size(req.getSize() == null ? "M" : req.getSize())
                .orElseThrow(() -> badRequest("Size không hợp lệ"));

        String doNgot = req.getDoNgot() == null ? "100%" : req.getDoNgot();
        if (!TuyChonMon.DO_NGOT.contains(doNgot)) {
            throw badRequest("Độ ngọt không hợp lệ");
        }

        List<String> toppingIds = new ArrayList<>(new LinkedHashSet<>(req.getToppings()));
        if (toppingIds.size() > TuyChonMon.TOPPING_TOI_DA) {
            throw badRequest("Tối đa " + TuyChonMon.TOPPING_TOI_DA + " topping");
        }

        long donGia = mon.getGia() + size.gia();
        List<String> moTa = new ArrayList<>();
        moTa.add(size.ten());
        if (!"100%".equals(doNgot)) moTa.add(doNgot + " đường");
        for (String id : toppingIds) {
            TuyChonMon.Topping t = TuyChonMon.topping(id).orElseThrow(() -> badRequest("Topping không hợp lệ: " + id));
            donGia += t.gia();
            moTa.add(t.ten());
        }

        MonDaChon line = new MonDaChon();
        line.setId(UUID.randomUUID().toString());
        line.setIdMon(mon.getIdMon());
        line.setTenMon(mon.getTenMon());
        line.setSize(size.id());
        line.setDoNgot(doNgot);
        line.setToppings(toppingIds);
        line.setMoTaTuyChon(String.join(" · ", moTa));
        line.setGhiChu(TextUtil.lamSach(req.getGhiChu(), 200));
        line.setSoLuong(soLuong);
        line.setDonGia(donGia);
        line.tinhThanhTien();
        return line;
    }

    /**
     * Chọn ảnh cho món, theo thứ tự:
     *  1. Ảnh trên mạng (http/https) trong DB -> giữ nguyên.
     *  2. Đường dẫn trong DB trỏ tới file CÓ THẬT trong thư mục ảnh (app.upload-dir) -> dùng file đó.
     *  3. Không thì dò file theo mã món: {idMon}.jpg / .jpeg / .png / .webp. Nhờ vậy dữ liệu gõ sai
     *     (vd. "/uploads11.jpg/", "/uploads/53" thiếu đuôi) vẫn hiện đúng ảnh, chỉ cần đặt file đúng tên.
     *  4. Không có file nào -> /uploads/{idMon}.jpg; trình duyệt tải lỗi sẽ tự hiện ảnh mặc định.
     * Thêm / đổi ảnh: chép file vào thư mục ảnh rồi tải lại trang, không cần khởi động lại server.
     */
    private MonAn chuanHoaAnh(MonAn monAn) {
        String img = monAn.getImage() == null ? "" : monAn.getImage().trim();
        if (img.startsWith("http://") || img.startsWith("https://")) return monAn;

        String tenFile = img.replaceFirst("^/?uploads/?", "").replaceAll("^/+|/+$", "");
        if (!tenFile.isEmpty() && coFile(tenFile)) {
            monAn.setImage("/uploads/" + tenFile);
            return monAn;
        }
        for (String duoi : DUOI_ANH) {
            if (coFile(monAn.getIdMon() + duoi)) {
                monAn.setImage("/uploads/" + monAn.getIdMon() + duoi);
                return monAn;
            }
        }
        monAn.setImage("/uploads/" + monAn.getIdMon() + ".jpg");
        return monAn;
    }

    /** File có trong thư mục ảnh (chặn tên kiểu "../" trỏ ra ngoài thư mục). */
    private boolean coFile(String tenFile) {
        Path p = thuMucAnh.resolve(tenFile).normalize();
        return p.startsWith(thuMucAnh) && Files.isRegularFile(p);
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
