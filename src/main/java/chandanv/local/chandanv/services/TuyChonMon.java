package chandanv.local.chandanv.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Bảng tuỳ chọn (size, topping, độ ngọt) và giá – nguồn giá duy nhất, frontend lấy qua /api/mon-an/tuy-chon. */
public final class TuyChonMon {

    public record Size(String id, String ten, int gia) {}

    public record Topping(String id, String ten, int gia) {}

    public static final List<Size> SIZES = List.of(
            new Size("M", "Size M", 0),
            new Size("L", "Size L", 5000));

    public static final List<Topping> TOPPINGS = List.of(
            new Topping("t1", "Trân châu đen", 5000),
            new Topping("t2", "Trân châu trắng", 5000),
            new Topping("t3", "Pudding trứng", 7000),
            new Topping("t4", "Kem cheese", 10000));

    public static final List<String> DO_NGOT = List.of("100%", "70%", "50%", "30%", "0%");

    public static final int SO_LUONG_TOI_DA = 99;
    public static final int TOPPING_TOI_DA = 4;

    private TuyChonMon() {}

    public static Optional<Size> size(String id) {
        return SIZES.stream().filter(s -> s.id().equalsIgnoreCase(id)).findFirst();
    }

    public static Optional<Topping> topping(String id) {
        return TOPPINGS.stream().filter(t -> t.id().equals(id)).findFirst();
    }

    public static Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sizes", SIZES);
        m.put("toppings", TOPPINGS);
        m.put("doNgot", DO_NGOT);
        m.put("soLuongToiDa", SO_LUONG_TOI_DA);
        return m;
    }
}
