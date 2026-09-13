package chandanv.local.chandanv.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import chandanv.local.chandanv.models.entity.GioHang;
import chandanv.local.chandanv.models.entity.GioHangItem;
import chandanv.local.chandanv.repositories.GioHangRepository;

@Service
@Transactional
public class GioHangService {

    private final GioHangRepository repo;

    public GioHangService(GioHangRepository repo) {
        this.repo = repo;
    }

    public GioHang getByIdBan(Integer idBan) {

        GioHang cart = repo.findByIdBan(idBan).orElse(null);

        if (cart == null) {
            GioHang newCart = new GioHang();
            newCart.setIdBan(idBan);
            newCart.setCreatedAt(Instant.now());
            newCart.setUpdatedAt(Instant.now());
            newCart.setItems(new ArrayList<>());
            return repo.save(newCart);
        }

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        return cart;
    }

    public GioHang update(Integer idBan, List<GioHangItem> items) {

        GioHang cart = getByIdBan(idBan);

        List<GioHangItem> newItems = new ArrayList<>();

        if (items != null) {
            for (GioHangItem item : items) {

                if (item.getSoLuong() == null || item.getSoLuong() <= 0)
                    continue;

                if (item.getId() == null || item.getId().isBlank())
                    item.setId(UUID.randomUUID().toString());

                if (item.getDonGia() == null)
                    item.setDonGia(0.0);

                item.setThanhTien(item.getSoLuong() * item.getDonGia());

                if (item.getThoiGianTao() == null)
                    item.setThoiGianTao(Instant.now());

                newItems.add(item);
            }
        }

        cart.setItems(newItems);
        cart.setUpdatedAt(Instant.now());

        return repo.save(cart);
    }

    public void delete(Integer idBan) {
        repo.deleteByIdBan(idBan);
    }

    public GioHang updateItemQuantity(Integer idBan, String itemId, Integer change) {

        GioHang cart = getByIdBan(idBan);

        if (change == null)
            change = 0;

        if (cart.getItems() == null)
            cart.setItems(new ArrayList<>());

        Iterator<GioHangItem> iterator = cart.getItems().iterator();

        while (iterator.hasNext()) {
            GioHangItem item = iterator.next();

            if (item.getId() != null && item.getId().equals(itemId)) {

                int currentQty = item.getSoLuong() == null ? 0 : item.getSoLuong();
                int newQty = currentQty + change;

                if (newQty <= 0) {
                    iterator.remove();
                } else {
                    item.setSoLuong(newQty);

                    Double donGia = item.getDonGia() == null ? 0.0 : item.getDonGia();
                    item.setThanhTien(newQty * donGia);
                }

                break;
            }
        }

        cart.setUpdatedAt(Instant.now());
        return repo.save(cart);
    }

    public GioHang removeItem(Integer idBan, Integer idMon) {

        GioHang gioHang = repo.findByIdBan(idBan).orElse(null);

        if (gioHang == null) {
            return null;
        }

        if (gioHang.getItems() != null) {
            gioHang.getItems().removeIf(i -> 
                i.getIdMon() != null && i.getIdMon().equals(idMon));
        }

        gioHang.setUpdatedAt(Instant.now());

        return repo.save(gioHang);
    }

    public GioHang addToCart(Integer idBan, GioHangItem newItem) {

        GioHang cart = getByIdBan(idBan);

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        if (newItem.getId() == null || newItem.getId().isBlank()) {
            newItem.setId(UUID.randomUUID().toString());
        }

        if (newItem.getSoLuong() == null || newItem.getSoLuong() <= 0) {
            newItem.setSoLuong(1);
        }

        if (newItem.getDonGia() == null) {
            newItem.setDonGia(0.0);
        }

        newItem.setThanhTien(newItem.getSoLuong() * newItem.getDonGia());
        newItem.setThoiGianTao(Instant.now());

        for (GioHangItem item : cart.getItems()) {

            if (item.getIdMon() != null &&
                item.getIdMon().equals(newItem.getIdMon())) {

                int currentQty = item.getSoLuong() == null ? 0 : item.getSoLuong();
                int newQty = currentQty + newItem.getSoLuong();

                item.setSoLuong(newQty);

                Double donGia = item.getDonGia() == null ? 0.0 : item.getDonGia();
                item.setThanhTien(newQty * donGia);

                cart.setUpdatedAt(Instant.now());
                return repo.save(cart);
            }
        }

        cart.getItems().add(newItem);
        cart.setUpdatedAt(Instant.now());

        return repo.save(cart);
    }

    public GioHang updateQuantity(Integer idBan, Integer idMon, Integer soLuong) {

        GioHang gioHang = getByIdBan(idBan);

        Iterator<GioHangItem> iterator = gioHang.getItems().iterator();

        while (iterator.hasNext()) {

            GioHangItem item = iterator.next();

            if (item.getIdMon() != null && item.getIdMon().equals(idMon)) {

                if (soLuong == null || soLuong <= 0) {
                    iterator.remove();
                } else {
                    item.setSoLuong(soLuong);

                    Double donGia = item.getDonGia() == null ? 0.0 : item.getDonGia();
                    item.setThanhTien(soLuong * donGia);
                }

                break;
            }
        }

        gioHang.setUpdatedAt(Instant.now());
        return repo.save(gioHang);
    }
}