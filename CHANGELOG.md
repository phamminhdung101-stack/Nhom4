# Nhật ký thay đổi

## Gộp hướng dẫn chạy vào README (29/10/2026)
- `HUONG_DAN_CHAY.md` gộp vào `README.md` rồi xoá, chỉ còn 1 tài liệu chính. Thứ tự: *Cần biết trước khi chạy* → mục lục → **Chạy project** (mục 1–9) → **Thiết kế hệ thống** (luồng nghiệp vụ, bảng bảo mật, API REST, giao thức WebSocket, cấu trúc mã: mục 10–14) → **Khi gặp sự cố** (mục 15–16). Phần trùng giữa 2 file giữ bản đầy đủ hơn.
- Sửa nội dung cũ của README:
  - Luồng "tạo đơn rồi mới xoá giỏ": thực tế lấy và xoá giỏ nguyên tử, tạo đơn lỗi thì trả lại giỏ.
  - `JOIN_TABLE` dùng mã riêng của máy, không phải mã QR.
  - Thêm bảng định tuyến đủ 6 sự kiện WebSocket (`CART_UPDATED`, `DEVICE_PENDING`, `LEFT_TABLE`…).
  - Giải thích ký hiệu quyền trong bảng API.
- Banner khởi động và chú thích `application.properties` nay trỏ tới `README.md` mục 6 / mục 7.

## Chạy trên máy khác: tiếng Việt trong console, cảnh báo IP nhầm (29/10/2026)
- **Log tiếng Việt thành `?` trên máy khác** (`Ti?m Trà ?? ch?y`): Spring Boot ghi log bằng bảng mã của console, mà nhiều máy Windows để 437 / 1252. Thêm `ConsoleUtf8`, gọi đầu tiên trong `main`:
  - Chạy `chcp 65001` để chuyển cửa sổ terminal sang UTF-8. Đã đo trên console thật: app chạy qua `mvnw` dùng chung console nên đổi được.
  - Đặt `logging.charset.console=UTF-8`, cho `System.out` / `System.err` ghi UTF-8.
  - Kiểm tra: log ra file hay console đều đúng UTF-8.
- **Cảnh báo "IP Wi-Fi đã đổi" nhầm khi copy project sang máy khác** (file `logs/ip-lan.txt` của máy cũ đi theo): file nay lưu `IP tên-máy`, chỉ cảnh báo khi cùng một máy đổi IP.
- File `.md`: đổi 30 ký tự gạch nối không ngắt dòng (U+2011, trong chữ "Wi-Fi") thành `-` thường, vì một số font / trình xem hiện thành ô vuông. Sửa link mục lục tương ứng.
- `HUONG_DAN_CHAY.md` mục 9: cách mang project sang máy khác (nên bỏ `target/`, `logs/`, `ssl/*.p12`; dữ liệu MongoDB không đi theo). Bảng lỗi: chữ `?` trong terminal, cảnh báo IP nhầm, mở `.md` bằng Notepad cũ.

## Điều cần biết lên đầu, dọn chú thích (28/10/2026)
- `HUONG_DAN_CHAY.md`: thêm mục **"⚠️ Cần biết trước khi chạy"** ngay đầu file: 15 điểm dev mới hay vấp, chia theo lúc (trước khi chạy / khi chạy / thử với điện thoại / khi sửa code), mỗi điểm có link tới mục chi tiết. `README.md`: dòng dẫn sang mục đó; sửa "Java 24" thành "Java 17+".
- Duyệt chú thích toàn bộ code (Java, JS, HTML, CSS, `application.properties`). **Không đổi logic**: biên dịch lại thành công, JS kiểm tra cú pháp đạt, giá trị cấu hình giống hệt bản trước.
  - **Sửa chú thích sai / lỗi thời:**
    - Giao thức `/ws` (`JOIN_TABLE` dùng mã riêng của máy, không phải mã QR; thêm các sự kiện giỏ hàng / duyệt máy).
    - Mã QR của bàn chỉ để xin vào bàn.
    - Địa chỉ `wss://…:8443`.
    - AuditLog ghi vào `logs/app.log`.
    - "Chỉ xoá giỏ sau khi lưu đơn": thực tế lấy và xoá giỏ nguyên tử, lỗi thì trả lại giỏ.
    - Bảng `lich_su_ban_hang` không dùng cho trang Doanh thu.
  - **Bỏ chú thích thừa:** chú thích kể lịch sử sửa đổi ("trước đây…", "như cũ", "không còn ổ D:"), dấu vết chỉnh sửa trong HTML ("THAY ĐỔI DUY NHẤT…", "BẮT ĐẦU/KẾT THÚC ĐOẠN FIX…"), chú thích CSS chỉ lặp lại code.
  - **Giải thích kỹ các chỗ quan trọng:**
    - Khoá lạc quan khi đổi trạng thái đơn và khi nhiều máy sửa giỏ (có ví dụ cụ thể).
    - Lấy giỏ nguyên tử khi gửi bếp; tranh chủ bàn khi 2 máy quét cùng lúc.
    - 4 điều kiện tự trả bàn; khái niệm phiên bàn.
    - Lọc màn hình bếp; tổng hợp doanh thu bằng `$facet` (múi giờ, đơn tại quầy).
    - Vì sao phải bọc session WebSocket, kiểm tra lại quyền khi máy bị mời ra, so sánh mã chống timing attack, `escapeHtml` chống XSS.
    - Vì sao HSTS phải tắt với chứng chỉ tự ký, vì sao tạo chứng chỉ trước Spring.
    - `server.address=0.0.0.0`, `auto-index-creation`, `no-cache`, ẩn stacktrace.

## Hướng dẫn chạy cho dev lần đầu (28/10/2026)
- `HUONG_DAN_CHAY.md` viết lại theo thứ tự làm: thêm **mục lục** và **tóm tắt 30 giây**; mục *Cài đặt* có link tải, lệnh `winget`, cách bật `JAVA_HOME`, cách kiểm tra (`java -version`, `Get-Service MongoDB`); mục *Chạy lần đầu* nói rõ lần đầu cần Internet và mất vài phút, cho Java qua tường lửa, log đúng trông thế nào; chạy bằng IntelliJ / VS Code (thư mục làm việc phải là gốc project).
- Thêm **"Thử toàn bộ luồng trong 5 phút"** chỉ với 1 máy tính (cửa sổ ẩn danh đóng vai khách, nút *Mở thử* ở trang Mã QR) theo đúng tên nút mới trên giao diện.
- Bảng lỗi thêm: thiếu `JAVA_HOME`, JDK cũ hơn 17, lỗi tải thư viện lần đầu, cổng bị chiếm, chữ tiếng Việt lỗi trong terminal, bếp không thấy đơn hôm qua, Doanh thu trống. Thêm mục *Tắt server · Dọn dẹp* (tìm tiến trình giữ cổng, xoá dữ liệu, gỡ chứng chỉ).
- `ssl/tao-chung-chi.bat`, `.sh`: ghi rõ **không cần chạy** (server tự tạo chứng chỉ).

## Chịu được dữ liệu lớn (28/10/2026)
Đo với 182.500 đơn giả (1 năm, 500 đơn/ngày, ~100 MB):
- **Màn hình bếp tải TOÀN BỘ đơn từ trước tới nay** mỗi lần cập nhật (~103 MB với 1 năm dữ liệu) → nay `GET /api/don-hang` mặc định chỉ lấy đơn hôm nay + đơn chưa xong 2 ngày gần nhất (đơn bỏ dở lâu ngày không chen vào), `?tu=` để xem ngày khác, tối đa 500 đơn → ~30 ms. Bỏ các truy vấn repository "lấy tất cả đơn". Tab *Tất cả* → *Tất cả hôm nay*, trang bếp ghi rõ xem đơn cũ ở trang Doanh thu.
- **Báo cáo doanh thu tải hết đơn vào Java rồi mới cộng** (30 ngày 2,3 s; 1 năm 14,3 s) → nay MongoDB tự tổng hợp bằng aggregation `$facet` (theo trạng thái, ngày, giờ, kênh, bàn, món, số ly) → 30 ngày 0,28 s; 1 năm 2,7 s. Kết quả **giống hệt** bản cũ (so 32 chỉ số trên 4 khoảng thời gian).
- Thêm index cho `trangThai` (đếm đơn đang chờ, lọc màn hình bếp).
- Log: ghi rõ giới hạn cắt file 10 MB, giữ 7 ngày, tổng tối đa 100 MB.
- `HUONG_DAN_CHAY.md`: mục *Dữ liệu lưu ở đâu*, ước tính dung lượng, cách sao lưu (`mongodump`).

## Giao diện dễ hiểu hơn (28/10/2026)
**Khách**
- Trang chào: bỏ nút *Đăng nhập / tích điểm* (không có hệ thống tích điểm thật, chỉ lưu SĐT trên máy → gây hiểu lầm), bỏ lựa chọn *Mang đi (Bàn x)* (không làm gì), bỏ mục *Liên hệ* trỏ tới phần không tồn tại. Thay bằng **thẻ bàn** (xanh: "Bạn đang ngồi: Bàn x – gọi món được rồi", vàng: "đang chờ nhân viên xác nhận", xám: "chưa quét mã QR"), nút lớn **"Xem thực đơn & gọi món"** và **hướng dẫn 3 bước** (Chọn món → Gửi bếp → Nhận món & thanh toán). Thêm mục *Đơn của tôi* trên menu.
- Thực đơn: khung **"Cách gọi món"** cho khách mới (bấm *Đã hiểu* thì ẩn hẳn); nút *Thêm* → **Thêm vào giỏ**; thanh dưới: *Đơn hàng* → **Đơn của tôi**, *Chat* → **Chat với quán**.
- Giỏ hàng: giải thích **giỏ chung của cả bàn**, nút **Chọn thêm món**, nút cuối trang *Xác nhận đặt món* → **Gửi bếp** (khớp hướng dẫn), hộp xác nhận *"Gửi đơn xuống bếp?"*.
- Theo dõi đơn: **câu giải thích theo từng trạng thái** bằng lời thường (đang chờ / đang pha / đang mang ra / xong – thanh toán tại bàn / đã huỷ); khi chưa có đơn hướng dẫn cách gửi; *Mã đơn hàng* → *Đơn đang theo dõi*; *Thanh toán* → *Gọi thanh toán*.

**Nhân viên**
- Bếp: dòng hướng dẫn quy trình (Nhận đơn → Pha chế → Mang ra → Hoàn thành, đơn trễ 15 phút hiện đỏ, huỷ đơn, bàn tự trả).
- Sơ đồ bàn: **chú thích ký hiệu** (trống / có khách / ⏳ máy chờ cho phép / 💬 tin chưa đọc); dấu ⏳ đổi sang màu cam để không nhầm với dấu tin nhắn đỏ; khung bên phải khi chưa chọn bàn liệt kê việc làm được (cho phép máy, huỷ đơn, chat, hoàn thành).
- POS: nút thùng rác có chữ **Xoá hết**, hướng dẫn cách thêm món, ghi chú đơn được gửi xuống bếp.

## Giải thích "Not secure" và lỗi PORT=8081 (28/10/2026)
- Tài liệu (`HUONG_DAN_CHAY.md`, `README.md`, chú thích `application.properties`, banner khởi động): thanh địa chỉ ghi **"Not secure"**, chữ `https` **bị gạch** là bình thường với chứng chỉ tự ký, kết nối vẫn mã hoá TLS 1.3. Bấm "Tiếp tục" không làm Chrome tin chứng chỉ. Thêm cách cho máy quản trị tin chứng chỉ (`keytool -exportcert` → `certutil -user -addstore Root`; gỡ bằng `certutil -user -delstore Root "Tiem Tra"`), lưu ý làm lại khi đổi IP và không áp dụng cho máy khách.
- Cảnh báo bẫy `PORT=8081`: HTTPS chiếm cổng 8081, cổng chuyển hướng bị tắt nên link / mã QR `http://<IP>:8081` cũ báo lỗi. Server nay tự in cảnh báo khi cổng HTTPS trùng cổng chuyển hướng; tài liệu có cách kiểm tra (`echo $env:PORT`) và xoá biến.

## Hướng dẫn cho dev khác (28/10/2026)
- Thêm `HUONG_DAN_CHAY.md`: cài đặt, chạy nhanh, server tự làm gì lần đầu, tài khoản & các trang, thử với điện thoại / iPhone, biến môi trường, đổi Wi-Fi, luồng nghiệp vụ, cấu trúc mã, bảo mật tóm tắt, xử lý lỗi thường gặp.
- `ChungChiHttps`: cắt khoảng trắng biến môi trường (CMD `set APP_HTTPS=false && …` lưu cả dấu cách cuối).
- Kiểm thử lại: 307 kịch bản đạt trên file `.jar` (HTTPS, dữ liệu mẫu gốc); DB trống (máy dev mới) và chế độ HTTP đều đạt.

## Chế độ dự phòng khi WebSocket bị chặn (iPhone + HTTPS tự ký) (28/10/2026)
- Trên iPhone mọi trình duyệt dùng WebKit; WebKit có thể chặn `wss://` với chứng chỉ tự ký (dù khách đã bấm "Truy cập trang web") → trước đây trang theo dõi đơn không tự cập nhật, chat không gửi được.
- `websocket.js`: không kết nối được → cứ 5 giây phát `RECONNECTED {polling:true}` để trang tự tải lại (trang theo dõi đơn, giỏ hàng, thanh giỏ, màn hình nhân viên đều xử lý sẵn); kết nối được lại thì thôi.
- Chat: API mới `POST /api/chat/{idBan}` (cùng kiểm tra như STOMP: máy đã duyệt, chống spam, 500 ký tự, tên người gửi do server đặt; nhân viên vẫn nhận ngay qua WebSocket). `customer-chat.js`: STOMP không kết nối được → gửi qua HTTP, lấy tin mới mỗi 4 giây, không hiện trùng tin; STOMP kết nối lại thì thôi.
- Thanh giỏ ở trang thực đơn chỉ nhấp nháy khi số món đổi (không nhấp nháy mỗi lần tải lại định kỳ).
- README: hướng dẫn quét QR trên iPhone / bằng Zalo, Facebook.

## Chạy được trên máy khác / Wi-Fi khác (28/10/2026)
- `pom.xml`: `java.version` 24 → **17** (mức tối thiểu của Spring Boot 3; code không dùng tính năng Java mới hơn) → máy cài JDK 17, 21, 24… đều build/chạy được.
- **DB trống tự nạp dữ liệu mẫu** từ `db_tiemtra/` (admin, loại món, món, khuyến mãi, nhân viên) → máy mới không cần `mongoimport`. Không nạp `ban_an` (bàn tạo mới với mã QR ngẫu nhiên riêng từng máy, không dùng mã công khai trong repo) và đơn hàng thử nghiệm.
- **Không kết nối được MongoDB** → khung "APPLICATION FAILED TO START" kèm hướng dẫn cài / bật MongoDB, `MONGODB_URI` (`MongoFailureAnalyzer`), thay vì stacktrace dài.
- **IP Wi-Fi đổi so với lần chạy trước** (đổi mạng / router cấp IP khác) → cảnh báo in lại mã QR (IP lưu ở `logs/ip-lan.txt`). Chứng chỉ HTTPS tự tạo lại cho IP mới.
- Đã thử: chép project sang thư mục khác (đường dẫn có dấu cách + tiếng Việt), không kèm `target/ ssl/ logs/ uploads/`, MongoDB trống → chạy bằng `mvnw spring-boot:run`, mọi chức năng đạt.
- README: sửa bảng địa chỉ bị vỡ, viết lại mục chạy dự án.

## Bật HTTPS/WSS mặc định (28/10/2026)
- Server chạy `https://<IP>:8443`, WebSocket `wss://` (TLS 1.3). Trước đây phải bật tay bằng profile `https` và tự chạy `keytool` (máy không có `keytool` trong PATH → script không chạy được).
- `ChungChiHttps` (gọi trong `main`): tự tạo chứng chỉ tự ký `ssl/tiemtra.p12` bằng `keytool` có sẵn trong JDK, gồm `localhost`, `127.0.0.1` và **IP Wi-Fi hiện tại**; tự tạo lại khi đổi mạng (IP khác), chứng chỉ hỏng hoặc còn < 30 ngày. Không tạo được → tự chạy HTTP, server không chết.
- `HttpsConfig`: cổng HTTP 8081 chỉ để **chuyển hướng sang HTTPS** (giữ nguyên đường dẫn + tham số → mã QR in trước đây vẫn dùng được); API không trả dữ liệu qua HTTP.
- Tắt HTTPS: `APP_HTTPS=false` (chạy HTTP cổng 8081 như cũ, dùng để so sánh Wireshark). HSTS vẫn tắt (chứng chỉ tự ký).
- CSP, CORS, `app.js` (Live Server), trang Mã QR, banner khởi động, README cập nhật theo `https://…:8443`.

## Kiểm thử lại lần 2 & sửa lỗi (28/10/2026)
Kiểm thử: build file `.jar` và chạy chính file đó trên DB nạp lại từ dữ liệu mẫu gốc; cú pháp mọi JS/HTML; 23 file tĩnh + header bảo mật + mọi tham chiếu trong trang; 278 kịch bản API/WebSocket/STOMP. Lỗi đã sửa:
- **Giỏ chưa gửi của khách cũ còn lại cho khách mới** sau khi bàn về Trống → khách mới thấy / gửi nhầm món của người trước. Nay kết thúc phiên thì xoá giỏ của bàn (`BanAnService.sauKhiKetThucPhien`).
- **Bàn tự về Trống khi khách đang chọn thêm món** (đơn đầu vừa hoàn thành) → mất giỏ đang chọn, khách gửi tiếp bị chặn. Nay còn món trong giỏ thì chưa tự trả bàn.
- **Máy bị mời ra / hết phiên vẫn nhận sự kiện bàn và đọc được chat mới** qua kết nối cũ. Nay WebSocket `/ws` đưa máy đó ra khỏi phòng bàn (`LEFT_TABLE`), chat STOMP lọc tin gửi đi (vẫn cho qua tin "phiên chat đã đặt lại").
- Log ghi ERROR kèm stacktrace mỗi khi khách đóng tab lúc ảnh chưa tải xong (`ClientAbortException`) → nay chỉ ghi mức debug.

## Chặn người lạ quét QR đặt món vào bàn người khác (28/10/2026)
- Trước đây ai có ảnh / quét được QR của bàn đều đặt món vào bàn đó (mã QR không bao giờ hết hạn) → người lạ có thể đặt rồi bỏ đi để khách thật trả tiền.
- **Phiên bàn**: mã in trong QR chỉ để xin vào bàn (`POST /api/ban-an/{id}/vao-ban`); mỗi điện thoại nhận mã riêng (`ThietBi`, lưu trong `BanAn.thietBi`). Máy đầu tiên ở bàn trống = chủ bàn (dùng ngay); **máy thứ 2 trở đi chờ nhân viên cho phép** ở Sơ đồ bàn (sự kiện `DEVICE_PENDING`, âm báo 3 tiếng "tít", dấu ⏳ trên sơ đồ, nút *Cho phép / Từ chối / Mời ra*). Máy chờ duyệt không thêm món, đặt đơn, chat, xem đơn hay nhận sự kiện của bàn được; trang chào hiện màn hình "Chờ nhân viên xác nhận" và tự hỏi lại mỗi 3 giây.
- Bàn về Trống = phiên kết thúc: mã của mọi máy hết hiệu lực (không đặt món từ xa được nữa), nhưng máy của phiên vừa xong vẫn xem lại được đơn của mình; trang chào không tự chiếm lại bàn, khách muốn gọi thêm bấm "Vào bàn lại".
- Đơn ghi máy đặt (`nguoiDat`, hiện "📱 Máy 2" trên màn hình bếp); người huỷ ghi "Khách Máy n (Bàn x)". Tối đa 10 máy chờ duyệt mỗi bàn. Máy bị mời ra không chat tiếp được.
- `BanAnService` đổi sang cập nhật từng trường (không ghi đè cả bàn) để nhiều máy quét cùng lúc không mất dữ liệu; 3 máy quét bàn trống cùng lúc → đúng 1 chủ bàn.
- Tắt bước duyệt khi demo: `app.ban.duyet-may-moi=false` (biến môi trường `APP_DUYET_MAY_MOI=false`).

## Nhiều điện thoại cùng 1 bàn (28/10/2026)
- Giỏ hàng vốn dùng chung theo bàn; nay **cập nhật realtime** giữa các máy: thêm/sửa/xoá món → sự kiện `CART_UPDATED` gửi tới phòng `TABLE_{idBan}`; trang thực đơn (thanh giỏ) và trang giỏ hàng của máy kia tự cập nhật.
- Một máy gửi bếp → các máy khác nhận `NEW_ORDER` (toast "máy khác cùng bàn đã gửi đơn…"), giỏ về trống, hộp xác nhận đang mở tự đóng; trang theo dõi đơn tự thêm đơn mới.
- Chat: mỗi máy có `clientId` riêng → tin của máy khác hiện là **"Khách cùng bàn"** thay vì "Bạn".

## Âm báo đơn mới (28/10/2026)
- Trước đây chỉ màn hình Bếp có âm báo đơn mới, và thường **không kêu** vì trình duyệt chặn âm thanh khi chưa bấm vào trang.
- Nay âm báo dùng chung cho **mọi trang quản trị** (`admin.js`), 3 giai điệu khác nhau: 🛎️ **đơn mới** (chuông 3 nốt đi lên, vang 2 lần), 💬 **tin nhắn** (2 nốt nhẹ), ❌ **huỷ đơn** (2 nốt trầm đi xuống). Trang không có thông báo đơn riêng (Sơ đồ bàn, Doanh thu, Mã QR) hiện thêm toast đơn mới, bấm vào mở màn hình bếp.
- Nút **Âm báo: Bật/Tắt** ở sidebar (công tắc trên màn hình bếp dùng chung cài đặt). Khi trình duyệt đang chặn âm thanh, hiện nút đỏ "bấm vào đây để bật âm báo"; bấm bất kỳ đâu trên trang cũng mở khoá.
- `websocket.js` phát mỗi sự kiện đơn thành window event `order-socket`; trang không tự mở WebSocket đơn (Mã QR) được `admin.js` mở giúp 1 kết nối để vẫn nghe âm báo.

## Kiểm thử lại & sửa lỗi (28/10/2026)
Kiểm thử: cú pháp mọi file JS/HTML, dò id/hàm bị gãy trên 10 trang, 110 kịch bản API (mọi endpoint trong README + chạy song song). Lỗi đã sửa:
- **Đăng nhập máy thứ 2 làm máy thứ 1 bị đăng xuất** (vd. bếp và POS cùng tài khoản): mỗi lần đăng nhập cấp token mới đè token cũ → nay token còn hạn thì dùng lại, chỉ gia hạn (`AdminService`).
- **Bấm "Gửi bếp" 2 lần / 2 điện thoại cùng bàn bấm cùng lúc tạo 2 đơn trùng**: lấy và xoá giỏ trong 1 thao tác nguyên tử (`findAndRemove`), lưu đơn lỗi thì trả lại giỏ (`DonHangService.datMonTuGio`).
- **2 điện thoại cùng bàn thêm món cùng lúc làm mất món**: giỏ có trường `phienBan`, chỉ ghi khi chưa bị ai sửa, nếu bị sửa thì đọc lại và làm lại (`GioHangService.suaGio`). Thử 12 lần thêm cùng lúc → đủ 12 món.
- Khách ở trang thực đơn không được báo khi quán huỷ đơn: tin hệ thống trong chat giờ có âm báo + số chưa đọc + toast (`customer-chat.js`).
- Nút chọn tâm trạng có thể lỗi khi hộp thoại chưa khởi tạo (`main.js`).

## Huỷ đơn, tự trả bàn, thông báo chat, tự mở web bằng IP Wi-Fi (28/10/2026)
- **Huỷ đơn** (trạng thái 4 = Đã huỷ, lưu `lyDoHuy`, `nguoiHuy`): API `POST /api/don-hang/{id}/huy`. Khách tự huỷ khi đơn còn *Chờ xác nhận*; bếp đã nhận thì khách *Nhắn quán xin huỷ* và nhân viên huỷ ở màn hình bếp / sơ đồ bàn. Đổi trạng thái nguyên tử (`findAndModify`) để khách huỷ và bếp nhận đơn cùng lúc không ghi đè nhau. Đơn huỷ không tính doanh thu/món bán chạy; có tin hệ thống trong chat của bàn.
- **Tự trả bàn**: khi mọi đơn của lượt khách đã hoàn thành/huỷ (và có ít nhất 1 đơn hoàn thành) → bàn tự về *Trống*, xoá phiên chat, sự kiện `TABLE_STATUS_UPDATED` có `tuDong: true`.
- **Thông báo khi khách chat** ở mọi trang quản trị (`admin.js`): toast bấm được để mở chat bàn đó, âm báo, số tin chưa đọc trên menu và tiêu đề tab (dùng chung giữa các trang/tab).
- **Tự lấy IP Wi-Fi và mở trình duyệt** khi chạy server (`LanBrowserLauncher`, tắt bằng `APP_OPEN_BROWSER=false`).
- **Cache file tĩnh**: `max-age=1h` → `no-cache` (`application.properties`). Trước đây trình duyệt giữ HTML/JS cũ tới 1 tiếng nên sửa code xong không thấy thay đổi; giờ luôn hỏi lại server (file không đổi trả 304).
- README: lệnh chạy trong PowerShell là `.\mvnw.cmd spring-boot:run`.
- **Khách xem mọi đơn đã đặt**: API `GET /api/don-hang/ban/{idBan}/luot-nay` (các đơn của lượt khách hiện tại); `tracking.html` có danh sách *Các đơn đã đặt*, mỗi đơn có trạng thái riêng và nút *Huỷ đơn* / *Nhắn quán xin huỷ*. Trả bàn giữ mốc `thoiGianDatBan` → khách vừa xong vẫn xem lại được, khách mới vào bàn thì không thấy đơn khách trước.
- **Báo cáo doanh thu chi tiết** (`dashboard.html`, menu *Doanh thu*; API `GET /api/don-hang/bao-cao?tu=&den=`): chọn nhanh Hôm nay / Hôm qua / 7 ngày / 30 ngày / Tháng này / Tháng trước hoặc tự chọn ngày (tối đa 1 năm). Doanh thu chỉ tính **đơn hoàn thành**, so sánh % với kỳ trước cùng độ dài; đơn đang xử lý và đơn huỷ thống kê riêng; số ly, trung bình/đơn; biểu đồ theo ngày và giờ cao điểm; kênh bán (QR tại bàn / POS tại quầy); bảng theo món (tỷ trọng), theo bàn, chi tiết đơn lọc theo trạng thái; **xuất CSV** mở bằng Excel.
- **Reset bàn theo lựa chọn** (`tables.html`): nút *Reset bàn…* mở hộp thoại tick từng bàn (nhanh: *Chọn bàn có khách* / *Chọn tất cả*). API `PUT /api/ban-an/reset` nhận `{idBans:[...]}`; không gửi body thì vẫn reset toàn bộ như cũ.
- **Bảo mật vừa sức đồ án sinh viên** (giữ đủ các lớp, nới ngưỡng, chỉnh trong `application.properties` nhóm `app.security.*`):
  - Giới hạn tần suất 300 → **1000** request/phút, 60 → **300** thao tác ghi/phút, 10 → **20** lần đăng nhập/phút; tắt hẳn bằng `APP_RATE_LIMIT=false` khi đo tải.
  - Khoá đăng nhập: sai **10** lần → khoá **1 phút** (trước 5 lần → 5 phút); IP sai **30** lần → khoá **5 phút** (trước 20 → 15 phút).
  - **HSTS mặc định tắt** (`app.security.hsts=false`): tránh việc chạy thử HTTPS một lần là `http://localhost` bị ép sang https cả năm.
  - CSP `connect-src` cho phép `localhost:*` (không ghi cứng cổng 8080); `app.js`, README, `qr_maker.html` đổi 8080 → **8081** cho khớp cổng server.
- Sửa: huỷ xong vẫn hiện "Chờ xác nhận" do trình duyệt dùng `app.js` cũ trong cache (chưa có trạng thái 4) → thêm `?v=` cho script ở `tracking.html`, `orders.html`, `tables.html`.

## Đổi phiên bản Java (26/10/2026 – tối)
- `pom.xml`: `java.version` 21 → **24** (theo JDK trên máy nhóm); `<release>` dùng chung biến `${java.version}`.

## Bổ sung bảo mật cơ bản (26/10/2026 – chiều)
- **Chống dò mật khẩu** (`LoginAttemptService`): sai 5 lần → khoá 5 phút theo tài khoản + IP, 20 lần/IP → khoá IP 15 phút; thông báo "còn N lần thử".
- **Chống đoán tài khoản qua thời gian phản hồi**: tài khoản không tồn tại vẫn chạy BCrypt với hash giả.
- **Giới hạn tần suất** (`RateLimitFilter` + `RateLimiter`): 300 req/phút, 60 thao tác ghi/phút, 10 lần đăng nhập/phút mỗi IP → `429` + `Retry-After`.
- **Giới hạn kích thước**: body API ≤ 1 MB (413); tin WebSocket/STOMP ≤ 16 KB; giới hạn bộ đệm gửi.
- **Chống spam WebSocket/chat**: `/ws` > 30 tin/10s → đóng kết nối; chat > 10 tin/10s → bỏ qua.
- **Header bảo mật** (`SecurityHeadersFilter`): CSP, X-Frame-Options, nosniff, Referrer-Policy, Permissions-Policy, no-store cho API, HSTS khi HTTPS.
- **Nhật ký bảo mật** (`AuditLog`, logger `AUDIT`, file `logs/app.log`): đăng nhập đúng/sai/khoá, mã QR sai, truy cập API trái phép, spam, bị chặn JOIN room.
- Ẩn stacktrace/thông báo lỗi nội bộ, giới hạn kích thước header/form (`application.properties`).
- **HTTPS/WSS tuỳ chọn**: profile `https` + script tạo chứng chỉ tự ký `ssl/tao-chung-chi.(bat|sh)`.
- Test: thêm `SecurityComponentsTest` (9 test) và test chống spam chat; E2E thêm 6 kịch bản bảo mật (48/48 đạt).

## Sửa lỗi & chuẩn hoá (26/10/2026 – sáng)
## Backend (Spring Boot)
- **Đơn hàng lưu kèm danh sách món** (`DonHang.items`). API mới `POST /api/don-hang/dat-mon/{idBan}` tạo đơn từ giỏ trên server, **chỉ xoá giỏ sau khi lưu đơn thành công**. Bỏ `chi_tiet_don` (không có idMon).
- **Giá do server tính**: `MonAnService.taoDong` = giá món trong DB + size + topping (`TuyChonMon`); bỏ qua mọi giá client gửi. Dòng giỏ có `id` riêng, gộp khi trùng món + trùng tuỳ chọn.
- **Mã đơn tăng dần, nguyên tử** (`SequenceService`, collection `counters`) thay `Random`. "Đơn mới nhất" sắp theo `thoiGian`. Thêm trường `idBan` rõ ràng (hết nhầm `idGioHang`).
- **Xác thực**: đăng nhập trả token (12h) → header `X-Admin-Token`; mật khẩu BCrypt (tự băm dữ liệu cũ). Mỗi bàn có mã bí mật trong QR → header `X-Table-Token`. Mọi API nhân viên/khách đều kiểm tra quyền.
- **Chat**: bỏ fallback "ghann"; STAFF cần `adminToken`, CUSTOMER cần `tableToken`; tên người gửi do server đặt; giới hạn 500 ký tự; không escape HTML 2 lần (client dùng textContent).
- **WebSocket /ws**: room KITCHEN / CASHIER / TABLE_{idBan}; JOIN cần token; mỗi session 1 room (hết nhận trùng); `ConcurrentWebSocketSessionDecorator` (gửi đa luồng an toàn); **Ping/Pong frame thật** mỗi 25s + đóng kết nối chết sau 60s; định tuyến đúng đối tượng.
- STOMP chat: heart-beat 10s/10s; bỏ SockJS.
- CORS cấu hình một chỗ (`app.cors.allowed-origins`) thay `@CrossOrigin("*")`; origin WebSocket giới hạn.
- Ảnh món: `app.upload-dir` (mặc định `./uploads`) thay `D:/Anh_do_an`; URL ảnh tương đối.
- `GlobalExceptionHandler`: lỗi trả JSON `{status, message}` (400/401/403/404/409) thay vì 500.
- `DataInitializer`: tạo bàn, sửa `maQR` bị ghi đè, cấp mã QR, băm mật khẩu, chuyển đơn cũ sang `idBan`, khởi tạo bộ đếm.
- Bật `auto-index-creation` (unique index có hiệu lực); bỏ `@Transactional` vô tác dụng; constructor injection toàn bộ.
- Gộp `BanAnController` + `BanAnApiController`; sửa lỗi lưu ảnh QR ghi đè link; thêm `doi-ma-qr`.
- API mới: `/api/mon-an/tuy-chon`, `/api/don-hang/tai-quay`, `/api/don-hang/thong-ke`, `/api/admin/me`.
- `pom.xml`: thêm `spring-security-crypto`, bỏ Thymeleaf.
- Test: viết lại thành unit test thuần (không cần MongoDB): `ChatSecurityTest`, `MonAnServiceTest`, `AdminServiceTest`.

## Frontend
- Chuyển toàn bộ vào `src/main/resources/static` → Spring Boot phục vụ trực tiếp, chạy được qua IP LAN trên điện thoại.
- `app.js` dùng chung: tự xác định địa chỉ backend (hết `localhost` cứng), gọi API có xử lý lỗi + token, `escapeHtml`, toast thay `alert`, chống bấm 2 lần.
- `websocket.js`: sửa lỗi Bếp/Thu ngân vào nhầm room CUSTOMER; tự kết nối lại (backoff 1s→30s), tải lại dữ liệu khi kết nối lại.
- Menu: dựng danh sách 1 lần (không `innerHTML +=`), ảnh lazy-load, ảnh lỗi chỉ thay 1 lần (hết vòng lặp request vô hạn), tìm kiếm debounce, skeleton, tuỳ chọn lấy từ server; viết các hàm còn thiếu (`handleMood`, `requestService`).
- Giỏ hàng: sửa/xoá theo id dòng, cập nhật mượt; đặt món báo lỗi đúng và giữ giỏ khi lỗi.
- Tracking: giờ đặt đúng (`thoiGian`), hiện danh sách món, nhận trạng thái realtime chỉ của bàn mình; nút gọi phục vụ/đánh giá gửi tin nhắn thật.
- Welcome: thêm Bootstrap JS bị thiếu, xác thực mã QR, xoá mã khỏi URL, bỏ tích điểm giả.
- Trang quản trị: CSS/sidebar dùng chung (`admin.css`, `admin.js`), bắt buộc đăng nhập, đăng xuất; Bếp hiển thị món + ghi chú + thời gian chờ + âm báo; Sơ đồ bàn hiện món của đơn; Dashboard số liệu thật + Chart.js; QR có mã bí mật, cảnh báo khi dùng localhost, in tất cả, đổi mã; **POS** mới (`admin/pos.html`) lấy thực đơn từ API, tạo đơn thật gửi bếp, in hoá đơn theo dữ liệu server.
- Toàn bộ dữ liệu người dùng hiển thị qua escape/textContent (chống XSS).

## Dọn dẹp
- Xoá: thư mục `chandanv/` (project thừa, package `java.*` bị cấm), `login.html` gốc (lỗi JS), `pages/pay.html`, `pages/pos.html` cũ, `mock_data/`, `templates/qrban.html`, `chi_tiet_don.json`.
- Dữ liệu mẫu: `ban_an` sửa `maQR` + thêm `token`; `gio_hang` để trống (cấu trúc cũ sai); `don_hang` thêm `idBan`, `items`.
- README viết lại (cách chạy, API, giao thức WebSocket, gợi ý Wireshark); nội dung DNS chuyển sang `docs/DNS.md`.
- Thêm `tests-e2e/` (Playwright + backend giả lập).
