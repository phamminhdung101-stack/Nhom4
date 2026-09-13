let allProducts = [];
let currentProduct = {};
let currentQuantity = 1;
let currentTableId = localStorage.getItem("idBan");
let cartData = { items: [] };

if (!currentTableId) {
  currentTableId = "0";
  localStorage.setItem("idBan", "0");
}

const API_MON_AN = "http://localhost:8080/api/mon-an";
const API_GIO_HANG = "http://localhost:8080/api/gio-hang";

const CATEGORY_MAP = {
  1: "tra-sua",
  2: "tra-nguyen-vi",
  3: "tra-trai-cay",
  4: "ca-phe",
  5: "matcha",
  6: "tui-tra",
  7: "topping",
};

const toppingsData = [
  { id: "t1", name: "Trân châu đen", price: 5000 },
  { id: "t2", name: "Trân châu trắng", price: 5000 },
  { id: "t3", name: "Pudding trứng", price: 7000 },
  { id: "t4", name: "Kem cheese", price: 10000 },
];

document.addEventListener("DOMContentLoaded", () => {
  const tableEl = document.getElementById("table-number");
  const params = new URLSearchParams(window.location.search);
  const idBanFromUrl = params.get("idBan");
  const idBanFromStorage = localStorage.getItem("idBan");

  if (tableEl) {
    if (idBanFromUrl) {
      tableEl.innerText = `Bàn ${idBanFromUrl}`;
      localStorage.setItem("idBan", idBanFromUrl);
    } else if (idBanFromStorage) {
      tableEl.innerText = `Bàn ${idBanFromStorage}`;
    } else {
      tableEl.innerText = "Mang đi";
    }
  }

  updateBottomCartNav();
  fetchData();

  document.querySelectorAll("[data-loai]").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.preventDefault();
      document
        .querySelectorAll(".sidebar-link, .btn-custom")
        .forEach((el) => el.classList.remove("active"));
      btn.classList.add("active");

      const loai = btn.dataset.loai;
      filterMenu(loai === "all" ? "all" : Number(loai));
    });
  });
});

async function fetchData() {
  const res = await fetch(API_MON_AN);
  const data = await res.json();

  allProducts = data.map((m) => ({
    id: m.idMon,
    name: m.tenMon,
    price: m.gia,
    image: m.image && m.image !== "" ? m.image : "assets/img/default.jpg",
    idLoai: m.idLoai,
    discount: m.giamGia || 0,
    bestSeller: m.banChay || false,
    description: m.moTa || "",
    category: CATEGORY_MAP[m.idLoai],
  }));

  renderMenu(allProducts);
}

function renderMenu(list) {
  const container = document.getElementById("menu-container");
  container.innerHTML = "";

  if (!list.length) {
    container.innerHTML = `
      <div class="col-12 text-center text-muted">
        Không có món trong danh mục này
      </div>`;
    return;
  }

  list.forEach((p) => {
    const hasDiscount = p.discount > 0;
    const finalPrice = (p.price * (100 - p.discount)) / 100;

    container.innerHTML += `
      <div class="col-6 col-md-4 col-lg-3">
        <div class="card product-card h-100" onclick="showProductDetail(${p.id})">
          <div class="card-img-wrapper position-relative">
            <img src="${p.image}" class="img-fluid" onerror="this.src='assets/img/default.jpg'">
            ${
              hasDiscount
                ? `<span class="badge bg-danger position-absolute top-0 end-0 m-2">-${p.discount}%</span>`
                : ""
            }
            ${
              p.bestSeller
                ? `<span class="badge bg-warning text-dark position-absolute top-0 start-0 m-2">BEST SELLER</span>`
                : ""
            }
          </div>
          <div class="text-center mt-2">
            <div class="product-name">${p.name}</div>
            <div class="fw-bold text-primary">${formatMoney(finalPrice)}</div>
          </div>
        </div>
      </div>
    `;
  });
}

function filterMenu(idLoai) {
  const titleEl = document.getElementById("current-category-title");
  const TITLE_MAP = {
    all: "Tất cả sản phẩm",
    1: "Trà sữa",
    2: "Trà nguyên vị",
    3: "Trà trái cây",
    4: "Cà phê",
    5: "Matcha",
    6: "Túi trà",
    7: "Topping",
  };

  titleEl.innerText = TITLE_MAP[idLoai] || "Danh sách món";

  if (idLoai === "all") renderMenu(allProducts);
  else renderMenu(allProducts.filter((p) => p.idLoai === idLoai));
}

function searchProduct() {
  const keyword = document.getElementById("search-input").value.toLowerCase();
  renderMenu(
    allProducts.filter((p) => p.name.toLowerCase().includes(keyword)),
  );
}

function showProductDetail(id) {
  currentProduct = allProducts.find((p) => p.id === id);
  if (!currentProduct) return;

  currentQuantity = 1;
  document.getElementById("modal-name").innerText = currentProduct.name;
  document.getElementById("modal-img").src = currentProduct.image;
  document.getElementById("modal-desc").innerText =
    currentProduct.description;
  document.getElementById("modal-quantity").innerText = 1;
  document.getElementById("modal-note").value = "";

  renderOptions();
  updateTotalPrice();

  bootstrap.Modal.getOrCreateInstance(
    document.getElementById("productModal"),
  ).show();
}



function renderOptions() {
  document.getElementById("size-options").innerHTML = `
    <input type="radio" class="btn-check" name="size" value="0" checked onchange="updateTotalPrice()">
    <label class="btn btn-outline-custom rounded-pill">Size M</label>
    <input type="radio" class="btn-check" name="size" value="5000" onchange="updateTotalPrice()">
    <label class="btn btn-outline-custom rounded-pill">Size L (+5k)</label>
  `;

  document.getElementById("sugar-options").innerHTML = ["100%", "70%", "50%", "30%", "0%"]
    .map(
      (l, i) =>
        `<input type="radio" class="btn-check" name="su" id="su${i}" ${i === 0 ? "checked" : ""}>
         <label class="btn btn-outline-custom rounded-pill me-1" for="su${i}">${l}</label>`,
    )
    .join("");

  document.getElementById("topping-list").innerHTML = toppingsData
    .map(
      (t) =>
        `<div class="d-flex justify-content-between mb-1">
          <div class="form-check">
            <input class="form-check-input" type="checkbox" value="${t.price}" onchange="updateTotalPrice()">
            <label class="form-check-label">${t.name}</label>
          </div>
          <small>+${formatMoney(t.price)}</small>
        </div>`,
    )
    .join("");
}

function updateTotalPrice() {
  let p = currentProduct.price;
  if (currentProduct.discount > 0)
    p = (p * (100 - currentProduct.discount)) / 100;

  const size = document.querySelector('input[name="size"]:checked');
  if (size) p += parseInt(size.value);

  document.querySelectorAll("#topping-list input:checked").forEach((c) => {
    p += parseInt(c.value);
  });

  document.getElementById("modal-base-price").innerText = formatMoney(p);
  document.getElementById("modal-total-price").innerText = formatMoney(
    p * currentQuantity,
  );
}

function updateQuantity(n) {
  if (currentQuantity + n > 0) {
    currentQuantity += n;
    document.getElementById("modal-quantity").innerText = currentQuantity;
    updateTotalPrice();
  }
}

async function addToCart() {
  try {
    let donGia = currentProduct.price;

    if (currentProduct.discount > 0)
      donGia = (donGia * (100 - currentProduct.discount)) / 100;

    const size = document.querySelector('input[name="size"]:checked');
    if (size) donGia += parseInt(size.value);

    document.querySelectorAll("#topping-list input:checked").forEach((c) => {
      donGia += parseInt(c.value);
    });

    const idBan = parseInt(localStorage.getItem("idBan") || "0");

   const response = await fetch(
  `${API_GIO_HANG}/${idBan}/add`,

      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          idMon: String(currentProduct.id),
          tenMon: currentProduct.name,
          soLuong: currentQuantity,
          donGia: donGia
        })
      }
    );

    if (!response.ok) {
      console.error("Server error:", response.status);
      alert("Không thêm được món!");
      return;
    }

    await updateBottomCartNav();

    showToast("Đã thêm món vào giỏ thành công!");

    bootstrap.Modal.getInstance(
      document.getElementById("productModal"),
    ).hide();

  } catch (error) {
    console.error("Lỗi thêm giỏ:", error);
    alert("Không kết nối được server!");
  }
}

async function updateBottomCartNav() {
  const idBan = localStorage.getItem("idBan") || "0";

  try {
    const res = await fetch(`${API_GIO_HANG}/${idBan}`);
    if (!res.ok) {
      // nếu chưa có giỏ
      resetBottomCart();
      return;
    }

    const data = await res.json();
    const items = data.items || [];

    if (!items.length) {
      resetBottomCart();
      return;
    }

    const totalCount = items.reduce((s, i) => s + i.soLuong, 0);
    const totalMoney = items.reduce(
      (s, i) => s + i.soLuong * i.donGia,
      0,
    );

    const badge = document.querySelector(".navbar.fixed-bottom .badge");
    if (badge) badge.innerText = totalCount;

    const total = document.querySelector(
      ".navbar.fixed-bottom .btn-primary span:last-child",
    );
    if (total) total.innerText = formatMoney(totalMoney);

  } catch (error) {
    console.error("Lỗi load cart:", error);
    resetBottomCart();
  }
}

function resetBottomCart() {
  const badge = document.querySelector(".navbar.fixed-bottom .badge");
  if (badge) badge.innerText = "0";

  const total = document.querySelector(
    ".navbar.fixed-bottom .btn-primary span:last-child",
  );
  if (total) total.innerText = "0đ";
}

function formatMoney(n) {
  return n.toLocaleString("vi-VN") + "đ";
}


function showToast(message) {
  const toast = document.createElement("div");
  toast.className = "custom-toast";
  toast.innerHTML = `<span>${message}</span>`;
  document.body.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    setTimeout(() => toast.remove(), 500);
  }, 3000);
}

async function changeQuantity(idMon, change) {

  const item = cartData.items.find(i => i.idMon === idMon);
  if (!item) return;

  let newQuantity = item.soLuong + change;

  if (newQuantity <= 0) {
    await removeItem(idMon);
    return;
  }

  const res = await fetch(`${API_GIO_HANG}/${currentTableId}/update`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      idMon: idMon,
      soLuong: newQuantity
    })
  });

  cartData = await res.json();

  loadCart();
  updateBottomCartNav();
}

async function removeItem(idMon) {

  const res = await fetch(`${API_GIO_HANG}/${currentTableId}/remove/${idMon}`, {
    method: "DELETE"
  });

  if (!res.ok) {
    alert("Không xoá được món");
    return;
  }

  cartData = await res.json();

  loadCart();
  updateBottomCartNav();
}

async function loadCart() {

  const idBan = parseInt(localStorage.getItem("idBan") || "0");

  const res = await fetch(`${API_GIO_HANG}/${idBan}`);
  cartData = await res.json();

  const items = cartData.items || [];

  const container = document.getElementById("cart-container");
  if (!container) return;

  container.innerHTML = "";

  items.forEach(item => {

    container.innerHTML += `
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden position-relative mb-2">

      <div class="position-absolute top-0 end-0 p-2">
        <button class="btn text-danger"
          onclick="removeItem(${item.idMon})">
          <i class="bi bi-trash"></i>
        </button>
      </div>

      <div class="d-flex p-2">

        <div class="flex-grow-1 ms-3 d-flex flex-column justify-content-between">

          <div>
            <div class="d-flex justify-content-between">
              <div class="fw-bold">${item.tenMon}</div>
              <div class="text-danger fw-bold">
                ${formatMoney(item.soLuong * item.donGia)}
              </div>
            </div>
          </div>

          <div class="d-flex align-items-center justify-content-center mt-2">

            <div class="d-flex align-items-center gap-3">

              <button class="btn btn-light rounded-circle"
                onclick="changeQuantity(${item.idMon}, -1)">
                -
              </button>

              <span class="fw-bold">${item.soLuong}</span>

              <button class="btn btn-light rounded-circle"
                onclick="changeQuantity(${item.idMon}, 1)">
                +
              </button>

            </div>

          </div>

        </div>

      </div>

    </div>
    `;

  });

}
