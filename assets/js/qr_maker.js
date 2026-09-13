console.log("QR MAKER JS LOADED");

const API_BASE = "http://localhost:8080";
const FRONTEND_BASE = "http://127.0.0.1:5500";

fetch(`${API_BASE}/api/ban-an`)
  .then(res => {
    if (!res.ok) throw new Error();
    return res.json();
  })
  .then(data => renderQR(data))
  .catch(() => alert("Không tải được danh sách bàn"));

function renderQR(banList) {
  const container = document.getElementById("qrContainer");
  container.innerHTML = "";

  if (!banList || banList.length === 0) {
    container.innerHTML = "<p>Chưa có bàn nào.</p>";
    return;
  }

  banList.forEach(ban => {
    const url = `${FRONTEND_BASE}/welcome.html?idBan=${ban.idBan}`;

    const card = document.createElement("div");
    card.className = "qr-card-preview";

    const qrDiv = document.createElement("div");

    card.innerHTML = `
      <div style="font-family: serif; font-weight: 700; color: #4e342e;">
        Tiệm Trà Thượng Hạng
      </div>

      <div class="qr-box"></div>

      <div>
        <div class="small text-uppercase text-muted fw-bold">Scan Me</div>
        <div style="font-size:1.8rem;font-weight:800;color:#d4af37">
          BÀN ${ban.idBan}
        </div>
      </div>

      <div class="d-flex gap-2 w-100 mt-2">
        <button class="btn btn-outline-secondary w-50 btn-link-qr">
          Link
        </button>
        <button class="btn btn-dark w-50 btn-save-qr">
          Lưu
        </button>
      </div>
    `;

    card.querySelector(".qr-box").appendChild(qrDiv);

    new QRCode(qrDiv, {
      text: url,
      width: 180,
      height: 180,
      correctLevel: QRCode.CorrectLevel.H,
    });

    setTimeout(() => {
      const canvas = qrDiv.querySelector("canvas");
      if (!canvas) return;

      canvas.style.cursor = "pointer";

      canvas.addEventListener("click", () => {
        window.location.href = url;
      });

      card.querySelector(".btn-link-qr").addEventListener("click", e => {
        e.stopPropagation();
        window.location.href = url;
      });

      card.querySelector(".btn-save-qr").addEventListener("click", e => {
        e.stopPropagation();

        const base64Image = canvas.toDataURL("image/png");

        //  TẢI ẢNH VỀ MÁY
        const link = document.createElement("a");
        link.href = base64Image;
        link.download = `QR_Ban_${ban.idBan}.png`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        //  LƯU CHÍNH ẢNH ĐÃ TẢI (BASE64 PNG) VÀO MONGODB
        fetch(`${API_BASE}/api/ban-an/luu-qr-image/${ban.idBan}`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            image: base64Image
          }),
        })
          .then(res => {
            if (!res.ok) throw new Error();
            alert(`Đã tải và lưu ảnh QR cho bàn ${ban.idBan}`);
          })
          .catch(() => alert("Không lưu được QR!"));
      });
    }, 0);

    container.appendChild(card);
  });
}