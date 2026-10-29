package chandanv.local.chandanv.websocket;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.models.entity.DonHang;
import chandanv.local.chandanv.models.entity.GioHang;
import chandanv.local.chandanv.models.entity.ThietBi;

/** Định tuyến sự kiện tới đúng room: Bếp/Thu ngân nhận mọi đơn, khách chỉ nhận đơn của bàn mình. */
@Component
public class OrderEventPublisher {

    private final OrderWebSocketHandler handler;

    public OrderEventPublisher(OrderWebSocketHandler handler) {
        this.handler = handler;
    }

    public void donMoi(DonHang don) {
        Map<String, Object> p = payload("NEW_ORDER", don);
        handler.sendToRooms(p, OrderWebSocketHandler.ROOM_KITCHEN, OrderWebSocketHandler.ROOM_CASHIER);
        // Các điện thoại khác cùng bàn cũng biết bàn vừa gửi đơn (giỏ chung đã chuyển thành đơn)
        if (don.getIdBan() != null && don.getIdBan() > 0) {
            handler.sendToRooms(p, OrderWebSocketHandler.tableRoom(don.getIdBan()));
        }
    }

    /** Máy bị mời ra / phiên bàn kết thúc -> kết nối cũ của máy đó thôi nhận sự kiện của bàn. */
    public void loaiMayHetQuyen(int idBan) {
        handler.loaiMayHetQuyen(idBan);
    }

    /** Có điện thoại mới xin vào bàn đang có khách -> nhân viên duyệt (chặn người lạ quét trộm QR). */
    public void mayChoDuyet(int idBan, String tenBan, ThietBi may) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "DEVICE_PENDING");
        p.put("idBan", idBan);
        p.put("tenBan", tenBan);
        p.put("may", may.getTen());
        handler.sendToRooms(p, OrderWebSocketHandler.ROOM_KITCHEN, OrderWebSocketHandler.ROOM_CASHIER);
    }

    /** Giỏ chung của bàn vừa thay đổi -> mọi điện thoại đang mở ở bàn đó cập nhật theo. gio = null: giỏ đã trống. */
    public void gioHangThayDoi(int idBan, GioHang gio) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "CART_UPDATED");
        p.put("idBan", idBan);
        p.put("tongSoLuong", gio == null ? 0 : gio.getTongSoLuong());
        p.put("tongTien", gio == null ? 0 : gio.getTongTien());
        handler.sendToRooms(p, OrderWebSocketHandler.tableRoom(idBan));
    }

    public void trangThaiDon(DonHang don) {
        Map<String, Object> p = payload("ORDER_STATUS_UPDATED", don);
        handler.sendToRooms(p, OrderWebSocketHandler.ROOM_KITCHEN, OrderWebSocketHandler.ROOM_CASHIER);
        if (don.getIdBan() != null && don.getIdBan() > 0) {
            handler.sendToRooms(p, OrderWebSocketHandler.tableRoom(don.getIdBan()));
        }
    }

    /** ban = null nghĩa là reset toàn bộ bàn. */
    public void banThayDoi(BanAn ban) {
        banThayDoi(ban, false);
    }

    /** tuDong = true: bàn tự trả về trống vì khách đã nhận đủ món (không do nhân viên bấm). */
    public void banThayDoi(BanAn ban, boolean tuDong) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "TABLE_STATUS_UPDATED");
        p.put("idBan", ban == null ? 0 : ban.getIdBan());
        p.put("trangThai", ban == null ? 0 : ban.getTrangThai());
        p.put("tuDong", tuDong);
        handler.sendToRooms(p, OrderWebSocketHandler.ROOM_KITCHEN, OrderWebSocketHandler.ROOM_CASHIER);
    }

    private static Map<String, Object> payload(String type, DonHang don) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", type);
        p.put("orderId", don.getId());
        p.put("idDonHang", don.getIdDonHang());
        p.put("tableId", don.getIdBan());
        p.put("status", don.getTrangThai());
        p.put("tongTien", don.getTongTien());
        p.put("soMon", don.getItems().stream().mapToInt(m -> m.getSoLuong()).sum());
        if (don.getTrangThai() != null && don.getTrangThai() == DonHang.DA_HUY) {
            p.put("lyDoHuy", don.getLyDoHuy());
            p.put("nguoiHuy", don.getNguoiHuy());
        }
        return p;
    }
}
