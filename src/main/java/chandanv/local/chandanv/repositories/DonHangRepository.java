package chandanv.local.chandanv.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import chandanv.local.chandanv.models.entity.DonHang;

public interface DonHangRepository extends MongoRepository<DonHang, String> {

    /** Đơn mới nhất của bàn – sắp theo thời gian tạo (không theo mã đơn). */
    Optional<DonHang> findFirstByIdBanOrderByThoiGianDesc(Integer idBan);

    // Không có truy vấn "lấy TẤT CẢ đơn": dữ liệu nhiều năm sẽ rất lớn -> luôn lọc theo thời gian (DonHangService.danhSach)

    /** Các đơn của bàn đặt từ thời điểm cho trước (lượt khách hiện tại), mới nhất trước. */
    List<DonHang> findByIdBanAndThoiGianGreaterThanEqualOrderByThoiGianDesc(Integer idBan, Instant tu);

    List<DonHang> findByThoiGianGreaterThanEqual(Instant tuNgay);

    Optional<DonHang> findFirstByOrderByIdDonHangDesc();

    long countByTrangThaiLessThan(Integer trangThai);

    /** Trong lượt khách hiện tại (đặt từ thời điểm tu), bàn còn đơn đang xử lý (trạng thái < 3) không. */
    boolean existsByIdBanAndTrangThaiLessThanAndThoiGianGreaterThanEqual(Integer idBan, Integer trangThai, Instant tu);

    /** Bàn đã có đơn hoàn thành (khách đã nhận món) kể từ thời điểm cho trước không. */
    boolean existsByIdBanAndTrangThaiAndThoiGianCapNhatGreaterThanEqual(Integer idBan, Integer trangThai, Instant tu);
}
