/**
 * main.js – trang thực đơn (index.html).
 * Giá hiển thị chỉ để tham khảo; giá thật do server tính khi thêm vào giỏ.
 */
(function () {
  "use strict";

  const TEN_LOAI = {
    all: "Tất cả sản phẩm",
    1: "Trà sữa",
    2: "Trà nguyên vị",
    3: "Trà trái cây",
    4: "Cà phê",
    5: "Matcha",
    6: "Túi trà / Quà tặng",
    7: "Topping",
  };

  // Gợi ý theo tâm trạng -> loại món
  const MOOD = {
    stress: { loai: 2, msg: "Một tách trà nguyên vị giúp bạn thư thái hơn" },
    sleepy: { loai: 4, msg: "Cà phê sẽ giúp bạn tỉnh táo ngay" },
    happy: { loai: 3, msg: "Trà trái cây tươi mát cho ngày vui" },
    love: { loai: 1, msg: "Trà sữa ngọt ngào cho người đang yêu" },
  };

  const state = {
    table: App.captureTableFromUrl(),
    products: [],
    options: { sizes: [], toppings: [], doNgot: [] },
    currentLoai: "all",
    keyword: "",
    current: null,
    qty: 1,
    cart: null,
  };

  const $ = (id) => document.getElementById(id);

  document.addEventListener("DOMContentLoaded", () => {
    const scan = App.getTableScan();
    $("table-number").textContent = state.table
      ? "Bàn " + state.table.idBan
      : scan ? "Bàn " + scan.idBan + " – chờ xác nhận" : "Chưa quét QR";

    document.querySelectorAll("[data-loai]").forEach((btn) =>
      btn.addEventListener("click", (e) => {
        e.preventDefault();
        filterMenu(btn.dataset.loai === "all" ? "all" : Number(btn.dataset.loai));
      })
    );

    $("search-input").addEventListener("input", App.debounce((e) => {
      state.keyword = e.target.value.trim().toLowerCase();
      render();
    }, 200));

    $("menu-container").addEventListener("click", (e) => {
      const card = e.target.closest("[data-id]");
      if (card) showProductDetail(Number(card.dataset.id));
    });
    $("menu-container").addEventListener("keydown", (e) => {
      const card = e.target.closest("[data-id]");
      if (card && (e.key === "Enter" || e.key === " ")) {
        e.preventDefault();
        showProductDetail(Number(card.dataset.id));
      }
    });

    $("productModal").addEventListener("change", updateTotalPrice);
    $("btn-add-to-cart").addEventListener("click", (e) => App.withBusy(e.currentTarget, addToCart));
    $("btn-go-cart").addEventListener("click", goToCart);

    if (location.hash === "#qua-tang") {
      state.currentLoai = 6;
      filterMenuButtons(6);
    }
    loadData();
    refreshCartBar();
    connectTable();
    huongDanGoiMon();
  });

  /** Khung "Cách gọi món": hiện cho khách mới, bấm "Đã hiểu" thì không hiện lại trên máy này. */
  function huongDanGoiMon() {
    const box = $("menu-guide");
    if (!box || App.store.get("daXemHuongDan")) return;
    box.classList.remove("d-none");
    $("menu-guide-close").addEventListener("click", () => {
      App.store.set("daXemHuongDan", true);
      box.classList.add("d-none");
    });
  }

  /** Nhiều điện thoại cùng bàn dùng CHUNG 1 giỏ: máy khác thêm/xoá món hoặc gửi bếp -> thanh giỏ cập nhật ngay. */
  function connectTable() {
    if (!state.table || !window.OrderSocket) return;
    OrderSocket.connect({
      join: { type: "JOIN_TABLE", idBan: state.table.idBan, token: state.table.token },
      onEvent(data) {
        if (data.type === "CART_UPDATED") updateCartBar(data);
        else if (data.type === "RECONNECTED") refreshCartBar();
        else if (data.type === "NEW_ORDER") {
          App.toast(`Bàn ${data.tableId} vừa gửi đơn #DH-${data.idDonHang} (${data.soMon} món) xuống bếp`, "success", 4500);
        }
      },
    });
  }

  async function loadData() {
    try {
      const [products, options] = await Promise.all([App.api("/api/mon-an"), App.api("/api/mon-an/tuy-chon")]);
      state.products = products.map((m) => ({
        id: m.idMon,
        name: m.tenMon || "",
        price: m.gia || 0,
        image: App.imageUrl(m.image),
        idLoai: m.idLoai,
      }));
      state.options = options;
      render();
    } catch (e) {
      $("menu-container").innerHTML = `
        <div class="col-12 text-center text-muted py-5">
          <i class="bi bi-wifi-off fs-1 d-block mb-2"></i>
          <p></p>
          <button class="btn btn-outline-primary rounded-pill" id="btn-retry">Thử lại</button>
        </div>`;
      $("menu-container").querySelector("p").textContent = e.message;
      $("btn-retry").addEventListener("click", loadData);
    }
  }

  function filterMenu(loai) {
    state.currentLoai = loai;
    filterMenuButtons(loai);
    render();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function filterMenuButtons(loai) {
    document.querySelectorAll("[data-loai]").forEach((b) =>
      b.classList.toggle("active", String(b.dataset.loai) === String(loai))
    );
    $("current-category-title").textContent = TEN_LOAI[loai] || "Danh sách món";
  }

  function render() {
    const list = state.products.filter(
      (p) =>
        (state.currentLoai === "all" || p.idLoai === state.currentLoai) &&
        (!state.keyword || p.name.toLowerCase().includes(state.keyword))
    );
    const container = $("menu-container");
    if (!list.length) {
      container.innerHTML = `<div class="col-12 text-center text-muted py-5">Không có món phù hợp</div>`;
      return;
    }
    // Dựng 1 lần rồi gán (nhanh hơn nhiều so với innerHTML += trong vòng lặp)
    container.innerHTML = list
      .map(
        (p) => `
      <div class="col-6 col-md-4 col-lg-3 fade-in">
        <div class="card product-card h-100" data-id="${p.id}" role="button" tabindex="0" aria-label="${App.escapeHtml(p.name)}">
          <div class="card-img-wrapper position-relative">
            <img src="${App.escapeHtml(p.image)}" class="img-fluid" loading="lazy" decoding="async"
                 alt="${App.escapeHtml(p.name)}" onerror="App.imgFallback(this)">
          </div>
          <div class="text-center mt-2">
            <div class="product-name">${App.escapeHtml(p.name)}</div>
            <div class="fw-bold text-primary">${App.formatMoney(p.price)}</div>
          </div>
        </div>
      </div>`
      )
      .join("");
  }

  function showProductDetail(id) {
    const p = state.products.find((x) => x.id === id);
    if (!p) return;
    state.current = p;
    state.qty = 1;
    $("modal-name").textContent = p.name;
    $("modal-img").src = p.image;
    $("modal-img").onerror = function () { App.imgFallback(this); };
    $("modal-quantity").textContent = "1";
    $("modal-note").value = "";
    renderOptions();
    updateTotalPrice();
    bootstrap.Modal.getOrCreateInstance($("productModal")).show();
  }

  function renderOptions() {
    const o = state.options;
    $("size-options").innerHTML = o.sizes
      .map(
        (s, i) => `
      <input type="radio" class="btn-check" name="size" id="size-${s.id}" value="${s.id}" ${i === 0 ? "checked" : ""}>
      <label class="btn btn-outline-custom rounded-pill" for="size-${s.id}">
        ${App.escapeHtml(s.ten)}${s.gia ? " (+" + s.gia / 1000 + "k)" : ""}
      </label>`
      )
      .join("");

    $("sugar-options").innerHTML = o.doNgot
      .map(
        (d, i) => `
      <input type="radio" class="btn-check" name="sugar" id="su${i}" value="${d}" ${i === 0 ? "checked" : ""}>
      <label class="btn btn-outline-custom rounded-pill me-1" for="su${i}">${d}</label>`
      )
      .join("");

    $("topping-list").innerHTML = o.toppings
      .map(
        (t) => `
      <div class="d-flex justify-content-between mb-1">
        <div class="form-check">
          <input class="form-check-input" type="checkbox" id="tp-${t.id}" value="${t.id}" data-gia="${t.gia}">
          <label class="form-check-label" for="tp-${t.id}">${App.escapeHtml(t.ten)}</label>
        </div>
        <small>+${App.formatMoney(t.gia)}</small>
      </div>`
      )
      .join("");
  }

  function selectedOptions() {
    const size = document.querySelector('input[name="size"]:checked');
    const sugar = document.querySelector('input[name="sugar"]:checked');
    return {
      size: size ? size.value : "M",
      doNgot: sugar ? sugar.value : "100%",
      toppings: [...document.querySelectorAll("#topping-list input:checked")].map((c) => c.value),
    };
  }

  function updateTotalPrice() {
    if (!state.current) return;
    const sel = selectedOptions();
    const size = state.options.sizes.find((s) => s.id === sel.size);
    let unit = state.current.price + (size ? size.gia : 0);
    sel.toppings.forEach((id) => {
      const t = state.options.toppings.find((x) => x.id === id);
      if (t) unit += t.gia;
    });
    $("modal-base-price").textContent = App.formatMoney(unit);
    $("modal-total-price").textContent = App.formatMoney(unit * state.qty);
  }

  function updateQuantity(n) {
    const next = state.qty + n;
    if (next < 1 || next > 99) return;
    state.qty = next;
    $("modal-quantity").textContent = next;
    updateTotalPrice();
  }

  /** Máy chưa được vào bàn: chưa quét QR, hoặc đang chờ nhân viên xác nhận. */
  function chuaVaoBan() {
    if (App.getTableScan()) {
      App.toast("Máy của bạn đang chờ nhân viên xác nhận vào bàn. Đang chuyển tới trang xác nhận…", "info", 3000);
      setTimeout(() => (window.location.href = "welcome.html"), 1200);
    } else {
      App.toast("Vui lòng quét mã QR trên bàn để gọi món.", "error", 4000);
    }
  }

  async function addToCart() {
    if (!state.table) return chuaVaoBan();
    const sel = selectedOptions();
    try {
      state.cart = await App.api(`/api/gio-hang/${state.table.idBan}/items`, {
        method: "POST",
        body: {
          idMon: state.current.id,
          soLuong: state.qty,
          size: sel.size,
          doNgot: sel.doNgot,
          toppings: sel.toppings,
          ghiChu: $("modal-note").value.trim(),
        },
      });
      updateCartBar(state.cart);
      bootstrap.Modal.getInstance($("productModal")).hide();
      App.toast("Đã thêm " + state.current.name + " vào giỏ", "success");
    } catch (e) {
      App.toast(e.message, "error", 4000);
    }
  }

  async function refreshCartBar() {
    if (!state.table) return updateCartBar(null);
    try {
      updateCartBar(await App.api(`/api/gio-hang/${state.table.idBan}`));
    } catch (e) {
      updateCartBar(null);
      if (e.status === 403) {
        App.toast("Mã QR đã hết hạn hoặc không đúng. Vui lòng quét lại mã trên bàn.", "error", 5000);
      }
    }
  }

  let soMonTruoc = null;
  function updateCartBar(cart) {
    const soMon = cart ? cart.tongSoLuong : 0;
    $("cart-count").textContent = soMon;
    $("cart-total").textContent = App.formatMoney(cart ? cart.tongTien : 0);
    if (soMon === soMonTruoc) return; // tải lại định kỳ mà không đổi -> không nhấp nháy
    soMonTruoc = soMon;
    const bar = $("btn-go-cart");
    bar.classList.remove("bump");
    void bar.offsetWidth; // khởi động lại animation
    if (soMon) bar.classList.add("bump");
  }

  function goToCart() {
    if (!state.table) return chuaVaoBan();
    window.location.href = "pages/cart.html";
  }

  function requestService(text) {
    if (!state.table || !window.CustomerChat) {
      App.toast("Vui lòng quét mã QR trên bàn để gọi phục vụ.", "error");
      return;
    }
    if (window.CustomerChat.send("Yêu cầu: " + text)) App.toast("Đã gửi yêu cầu: " + text, "success");
  }

  function handleMood(mood) {
    const m = MOOD[mood];
    bootstrap.Modal.getOrCreateInstance($("moodModal")).hide();
    if (!m) return;
    filterMenu(m.loai);
    App.toast(m.msg, "info", 3500);
  }

  // Các hàm được gọi từ thuộc tính onclick trong HTML
  Object.assign(window, { filterMenu, updateQuantity, requestService, handleMood });
})();
