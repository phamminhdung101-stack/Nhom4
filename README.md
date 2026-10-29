# Tiệm Trà – Order món qua QR với WebSocket

Đề tài: Nghiên cứu và xây dựng hệ thống giao tiếp thời gian thực cho hệ thống Order món ăn qua mã QR sử dụng WebSocket.

Khách quét QR trên bàn để gọi món bằng điện thoại. Nhân viên dùng các trang quản trị: bếp, thu ngân (POS), sơ đồ bàn kèm chat, doanh thu, in mã QR. Mọi thay đổi (đơn mới, trạng thái đơn, giỏ hàng, chat) được đẩy tới trình duyệt ngay lập tức qua WebSocket.

**Công nghệ:** Java 17+ · Spring Boot 3.5 · MongoDB · WebSocket (Raw + STOMP) · HTML/CSS/JS (Bootstrap 5) · Wireshark

**Tóm tắt 30 giây:** cài **JDK 17+** và **MongoDB** → mở terminal ở thư mục project → `.\mvnw.cmd spring-boot:run` → trình duyệt tự mở trang quản trị → đăng nhập `ghann` / `88888`.

---

## ⚠️ Cần biết trước khi chạy (đọc hết, khoảng 2 phút)

Đây là những điều dev mới hay vấp nhất. Mỗi dòng có link tới mục giải thích chi tiết.

**Trước khi chạy**
1. **Phải có JDK 17+ và MongoDB đang chạy.** Thiếu một trong hai thì server không lên. MongoDB phải chạy ở `localhost:27017` (đổi được bằng `MONGODB_URI`). → [mục 1](#1-cài-đặt-làm-1-lần)
2. **Lần chạy đầu cần Internet và mất vài phút** để tải thư viện. Không cần cài Maven.
3. **Chạy ở thư mục gốc project** (thư mục có `pom.xml`). Server đọc `db_tiemtra/`, `ssl/`, `uploads/` theo đường dẫn tương đối, chạy ở thư mục khác sẽ thiếu dữ liệu mẫu và ảnh.
4. **PowerShell phải gõ `.\mvnw.cmd`** (có `.\`), không gõ `mvnw.cmd`.
5. **Không đặt biến `PORT=8081`** khi chạy HTTPS: link và mã QR `http://...:8081` sẽ hỏng. → [mục 7](#7-cấu-hình-bằng-biến-môi-trường)

**Khi chạy**

6. **Mặc định là HTTPS ở cổng 8443**, dùng chứng chỉ **tự ký** do server tự tạo:
   - Mỗi máy / điện thoại vào lần đầu đều thấy cảnh báo *"Kết nối không riêng tư"*: bấm **Nâng cao → Tiếp tục**.
   - Sau đó thanh địa chỉ vẫn ghi **"Not secure"**. Đây **không phải lỗi**, dữ liệu vẫn được mã hoá. → [mục 6](#6-https-cảnh-báo-not-secure-là-gì)
   - Không muốn cảnh báo thì chạy HTTP: `APP_HTTPS=false`.
7. **Dữ liệu mẫu tự nạp khi database `tiemtra` còn trống**: tài khoản, thực đơn, 10 bàn. Không có đơn hàng mẫu, nên trang Doanh thu lúc đầu trống là đúng.
8. **Có giới hạn số request và khoá đăng nhập sai.** Bấm/gửi quá nhanh hoặc chạy test tự động sẽ bị báo `429`. Đó là tính năng bảo mật, không phải lỗi; khi test thì tắt bằng `APP_RATE_LIMIT=false`.

**Khi thử với điện thoại**

9. **Điện thoại phải cùng Wi-Fi** với máy chạy server, và Java phải được cho qua **Windows Firewall**. → [mục 4](#4-thử-với-điện-thoại)
10. **Mở trang quản trị bằng IP, không bằng `localhost`**: mã QR lấy địa chỉ từ trang đang mở, mở bằng `localhost` thì điện thoại quét sẽ không vào được.
11. **Mã QR chứa IP của máy chạy server.** Đổi Wi-Fi, đổi máy, hoặc router cấp IP khác thì **phải in lại mã QR**. → [mục 9](#9-đổi-wi-fi--chuyển-sang-máy-khác)
12. **Máy thứ 2 trở đi quét cùng một bàn phải chờ nhân viên bấm "Cho phép"** ở *Sơ đồ bàn*. Đây là chống người lạ đặt món vào bàn người khác. Khi demo thì tắt bằng `APP_DUYET_MAY_MOI=false`.
13. **Trình duyệt chặn âm thanh** cho tới khi bấm vào trang một lần. Ở màn hình bếp, bấm vào trang trước, nếu không đơn mới sẽ không kêu.

**Khi sửa code**

14. Sửa HTML/JS xong thì bấm **Ctrl + F5**. Sửa Java thì server tự khởi động lại (DevTools).
15. **Không đưa lên git:** `ssl/*.p12` (khoá riêng của chứng chỉ), `logs/`, `target/`. Đã có trong `.gitignore`, và server tự tạo lại khi chạy.
16. **Ảnh món nằm trong `uploads/` và có trên git** (tên file = mã món: `1.jpg` … `67.jpg`). Thêm / đổi ảnh thì commit cả file ảnh để cả nhóm có. → [mục 5](#5-tài-khoản-và-các-trang)

> Làm **lần lượt từ mục 1 đến mục 4** là chạy được và tự kiểm tra được máy mình chạy đúng. Các mục sau để tra cứu khi cần. Lịch sử thay đổi: `CHANGELOG.md`.

---

## Mục lục

**Chạy project**
1. [Cài đặt (làm 1 lần)](#1-cài-đặt-làm-1-lần)
2. [Chạy lần đầu](#2-chạy-lần-đầu)
3. [Thử toàn bộ luồng trong 5 phút (1 máy tính)](#3-thử-toàn-bộ-luồng-trong-5-phút-1-máy-tính)
4. [Thử với điện thoại](#4-thử-với-điện-thoại)
5. [Tài khoản và các trang](#5-tài-khoản-và-các-trang)
6. [HTTPS: cảnh báo "Not secure" là gì](#6-https-cảnh-báo-not-secure-là-gì)
7. [Cấu hình bằng biến môi trường](#7-cấu-hình-bằng-biến-môi-trường)
8. [Server tự làm gì · Dữ liệu lưu ở đâu](#8-server-tự-làm-gì--dữ-liệu-lưu-ở-đâu)
9. [Đổi Wi-Fi / chuyển sang máy khác](#9-đổi-wi-fi--chuyển-sang-máy-khác)

**Thiết kế hệ thống**

10. [Luồng nghiệp vụ](#10-luồng-nghiệp-vụ)
11. [Bảo mật đã áp dụng](#11-bảo-mật-đã-áp-dụng)
12. [API REST](#12-api-rest)
13. [Giao thức WebSocket](#13-giao-thức-websocket)
14. [Cấu trúc mã nguồn](#14-cấu-trúc-mã-nguồn)

**Khi gặp sự cố**

15. [Gặp lỗi thì làm gì](#15-gặp-lỗi-thì-làm-gì)
16. [Tắt server · Dọn dẹp](#16-tắt-server--dọn-dẹp)

---

## 1. Cài đặt (làm 1 lần)

| Phần mềm | Bắt buộc? | Ghi chú |
|---|---|---|
| **JDK 17 trở lên** | ✅ | 17, 21, 24… đều được. Phải là **JDK** (không phải JRE) |
| **MongoDB Community Server** | ✅ | Bản 6, 7 hoặc 8 |
| MongoDB Compass | tuỳ chọn | Giao diện xem / xoá dữ liệu |
| Git | tuỳ chọn | Nếu lấy project bằng `git clone` |
| Maven | ❌ không cần | Project có sẵn `mvnw` (Maven Wrapper) |

### 1.1 Cài JDK
- Tải tại https://adoptium.net (chọn **Temurin 21 LTS**, bản *.msi* cho Windows).
- Khi cài, **bật tuỳ chọn "Set JAVA_HOME variable"** (mặc định đang tắt, phải bấm vào để bật).
- Hoặc cài bằng lệnh: `winget install EclipseAdoptium.Temurin.21.JDK`

**Kiểm tra** (mở terminal **mới** sau khi cài):
```powershell
java -version          # phải ra version "17..." trở lên
echo $env:JAVA_HOME    # nên in ra thư mục JDK, vd. C:\Program Files\Eclipse Adoptium\jdk-21...
```

### 1.2 Cài MongoDB
- Tải tại https://www.mongodb.com/try/download/community (bản *.msi*).
- Khi cài chọn **Complete**, giữ tick **"Install MongoD as a Service"** để MongoDB tự bật cùng Windows. Có thể tick thêm **Install MongoDB Compass**.
- Hoặc cài bằng lệnh: `winget install MongoDB.Server` (và `winget install MongoDB.Compass.Full` nếu muốn có Compass).

**Kiểm tra** MongoDB đang chạy:
```powershell
Get-Service MongoDB    # cột Status phải là "Running"
```
Nếu thấy `Stopped`: `Win + R` → `services.msc` → *MongoDB Server* → *Start*.

Không cần cài thêm `mongoimport` hay `keytool`, cũng không cần tạo chứng chỉ bằng tay: server tự lo (xem mục 8).

---

## 2. Chạy lần đầu

**Bước 1 – Lấy project về:** `git clone https://github.com/phamminhdung101-stack/Nhom4.git`, hoặc giải nén file zip. Ảnh món có sẵn trong thư mục `uploads/`.

**Bước 2 – Mở terminal ở thư mục gốc project** (thư mục có file `pom.xml`). Trong VS Code: *Terminal → New Terminal*.

**Bước 3 – Chạy:**
```powershell
.\mvnw.cmd spring-boot:run        # PowerShell (Windows) – nhớ có .\ ở đầu
mvnw.cmd spring-boot:run          # CMD (Windows)
./mvnw spring-boot:run            # Linux / macOS
```

- **Lần đầu** cần Internet và mất **vài phút**, vì `mvnw` tải Maven và thư viện (khoảng vài trăm MB) về thư mục `~/.m2`. Từ lần sau chạy trong khoảng 10–20 giây, không cần mạng.
- Windows có thể hỏi **cho Java qua tường lửa**: chọn cho phép (**Private networks**), để điện thoại vào được.

**Bước 4 – Kết quả đúng.** Log phải có 2 dấu hiệu sau:
```
Tomcat started on ports 8443 (https), 8081 (http)
============================================================
 Tiệm Trà đã chạy!   IP Wi-Fi/LAN: 192.168.1.10
 Quản trị   : https://192.168.1.10:8443/admin
 Mã QR bàn  : https://192.168.1.10:8443/admin/qr_maker.html
 ...
============================================================
```
Trình duyệt **tự mở** trang quản trị. Lần đầu sẽ có cảnh báo *"Kết nối không riêng tư"*: bấm **Nâng cao → Tiếp tục**. Đây là bình thường với chứng chỉ tự ký (xem mục 6).

Đăng nhập **`ghann` / `88888`**.

> Log ghi `Tomcat started on port 8081 (https)` (chỉ 1 cổng) là sai. Terminal đang có biến `PORT=8081`, xem mục 15.

### Chạy bằng IDE (thay cho lệnh trên)
- **IntelliJ IDEA:** *File → Open* thư mục project, chờ IDE tải xong Maven, rồi bấm ▶ ở hàm `main` trong `ChandanvApplication.java`.
- **VS Code:** cài *Extension Pack for Java* + *Spring Boot Extension Pack*, mở thư mục project, rồi bấm *Run* trên `ChandanvApplication.java`.
- **Thư mục làm việc (working directory) phải là thư mục gốc project**, vì server đọc `db_tiemtra/`, `ssl/`, `uploads/` theo đường dẫn tương đối.
- Biến môi trường (mục 7) đặt trong *Run Configuration → Environment variables* (IntelliJ) hoặc mục `"env"` của `launch.json` (VS Code).

### Build file `.jar` để mang đi chạy
```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target\tiemtra-0.0.1-SNAPSHOT.jar      # chạy ở thư mục gốc project
```

---

## 3. Thử toàn bộ luồng trong 5 phút (1 máy tính)

Làm theo các bước sau để chắc chắn máy mình chạy đúng, chưa cần điện thoại.

1. **Nhân viên:** ở trang quản trị đã đăng nhập, mở menu **Đơn hàng (Bếp)**. **Bấm vào trang một lần** để trình duyệt cho phép phát âm báo (nút *Âm báo: Bật* ở sidebar).
2. **Đóng vai khách:** vào menu **Mã QR bàn**, ở *Bàn 01* bấm **Mở thử**. Copy địa chỉ của tab vừa mở, rồi dán vào một **cửa sổ ẩn danh** (`Ctrl + Shift + N`). Cửa sổ ẩn danh giống điện thoại của khách, không dùng chung phiên đăng nhập với nhân viên. Bấm qua cảnh báo chứng chỉ.
3. Trang chào hiện thẻ xanh **"Bạn đang ngồi: Bàn 1 – gọi món được rồi"**. Bấm **Xem thực đơn & gọi món** → chạm một món → **Thêm vào giỏ** → **Xem giỏ hàng** → **Gửi bếp** → **Gửi bếp ngay**.
4. **Màn hình bếp** phải **kêu chuông** và hiện đơn mới ngay lập tức. Bấm lần lượt **Nhận đơn → Xong → Đã giao**. Trang khách tự cập nhật từng bước.
5. Khi đơn *Hoàn thành*, **Bàn 1 tự về "Trống"** (xem ở menu **Sơ đồ bàn**).
6. Menu **Doanh thu** → chọn **Hôm nay**: thấy đơn vừa làm. **Chat**: khách bấm *Chat với quán* để nhắn, nhân viên trả lời ở *Sơ đồ bàn*.

Cả 6 bước đều chạy là máy đã sẵn sàng.

---

## 4. Thử với điện thoại

1. Điện thoại và máy chạy server phải **cùng mạng Wi-Fi**.
2. Trang quản trị phải mở **bằng IP** (trang tự mở đã dùng sẵn IP), **không dùng `localhost`**. Vào **Mã QR bàn** → **In tất cả**, hoặc quét thẳng mã trên màn hình.
3. Điện thoại quét QR → gặp cảnh báo chứng chỉ → *Nâng cao / Hiển thị chi tiết → Tiếp tục*.
4. Điện thoại không vào được: kiểm tra cùng Wi-Fi chưa, và đã cho **Java** qua **Windows Firewall** (mạng *Private*) chưa.

**Máy thứ 2 trở đi** quét QR cùng một bàn sẽ phải **chờ nhân viên bấm "Cho phép"** ở *Sơ đồ bàn* (bàn có dấu ⏳ màu cam). Bước này để chặn người lạ quét trộm QR rồi đặt món vào bàn người khác. Khi demo muốn bỏ bước này: `APP_DUYET_MAY_MOI=false` (mục 7).

**iPhone / quét bằng Zalo, Facebook:**
- Trên iPhone, mọi trình duyệt (Safari, Chrome, trình duyệt trong Zalo/Facebook…) đều dùng lõi WebKit, nên xử lý chứng chỉ tự ký giống Safari.
- Quét bằng app **Camera** → mở Safari/Chrome → *Hiển thị chi tiết → Truy cập trang web* (Chrome: *Nâng cao → Tiếp tục*) → vào được.
- Quét trong **Zalo / Facebook / Messenger**: trình duyệt trong app có thể chặn hẳn chứng chỉ tự ký. Khi đó chọn *Mở bằng Safari/Chrome*.
- WebKit có thể chặn WebSocket `wss://` với chứng chỉ tự ký. Khi đó trang khách **tự chuyển chế độ dự phòng**: đơn / giỏ tự tải lại mỗi 5 giây, chat gửi qua HTTP (`POST /api/chat/{idBan}`) và lấy tin mới mỗi 4 giây. Nhân viên vẫn nhận tin / đơn ngay như thường.
- Demo cho nhiều loại điện thoại thì nên chạy **HTTP** (`APP_HTTPS=false`): không có cảnh báo, chạy mượt nhất.

---

## 5. Tài khoản và các trang

Tài khoản mẫu (trong `db_tiemtra/tiemtra.admin.json`; lần khởi động đầu server tự băm BCrypt các mật khẩu này):

| Tài khoản | Mật khẩu |
|---|---|
| `ghann` | `88888` |
| `lhao` | `12345` |
| `mdung` | `11223` |

| Trang | Đường dẫn (sau `https://<IP>:8443`) | Dùng để |
|---|---|---|
| Đăng nhập | `/admin` | |
| Doanh thu | `/admin/dashboard.html` | Báo cáo theo ngày/giờ/món/bàn, xuất CSV |
| Bếp (KDS) | `/admin/orders.html` | Nhận đơn, chuyển trạng thái, huỷ đơn (chỉ hiện đơn **hôm nay** + đơn chưa xong 2 ngày gần nhất) |
| Thu ngân (POS) | `/admin/pos.html` | Tạo đơn mua tại quầy, in hoá đơn |
| Sơ đồ bàn + chat | `/admin/tables.html` | Xem bàn, **cho phép / từ chối máy xin vào bàn**, chat với khách, reset bàn. Dòng chú thích ở trên hiện **tổng** máy đang chờ duyệt và tin chưa đọc của mọi bàn; bấm vào để mở bàn cần xử lý |
| Mã QR bàn | `/admin/qr_maker.html` | In / tải / đổi mã QR từng bàn, nút *Mở thử* |
| Trang khách | `/welcome.html?idBan=1&t=<mã>` | Mở bằng cách **quét QR**, không gõ tay |

`http://<IP>:8081/...` tự chuyển sang HTTPS, nên mã QR in trước đây vẫn dùng được.
Ảnh món: đặt file `1.jpg`, `2.jpg`… (tên = mã món; nhận `.jpg`, `.jpeg`, `.png`, `.webp`) vào thư mục `uploads/` (đổi bằng `APP_UPLOAD_DIR`), rồi tải lại trang, không cần khởi động lại server. Đường dẫn ảnh lưu trong DB bị sai cũng không sao: server tự dò file theo mã món. Thiếu ảnh sẽ hiện ảnh mặc định. Ảnh trong `uploads/` được đưa lên git cùng code.

---

## 6. HTTPS: cảnh báo "Not secure" là gì

- Server chạy **HTTPS mặc định** (`https://<IP>:8443`, WebSocket `wss://`) với chứng chỉ **tự ký**. Chứng chỉ được tự tạo khi khởi động, có IP Wi-Fi hiện tại.
- Lần đầu mỗi máy / điện thoại sẽ cảnh báo *"Kết nối không riêng tư"*: bấm **Nâng cao → Tiếp tục**.
- Sau đó thanh địa chỉ **vẫn ghi "Not secure" và chữ `https` bị gạch**. Đây là **bình thường và vẫn an toàn**: kết nối vẫn mã hoá TLS 1.3, người khác cùng Wi-Fi không đọc được dữ liệu. Chrome chỉ *không tin* chứng chỉ tự ký; bấm "Tiếp tục" không làm Chrome tin chứng chỉ.
- Không thể có ổ khoá trên **mọi** máy khi chạy bằng IP nội bộ (`192.168.x.x`). Muốn vậy phải có tên miền thật + chứng chỉ thật.

**Cho máy tính quản trị tin chứng chỉ để hết chữ đỏ** (không cần quyền admin; chỉ áp dụng cho máy này):
```powershell
# 1. Lấy chứng chỉ ra file .cer (ở thư mục gốc project, server đã chạy ít nhất 1 lần)
& "$env:JAVA_HOME\bin\keytool.exe" -exportcert -alias tiemtra -keystore ssl\tiemtra.p12 -storepass tiemtra123 -rfc -file ssl\tiemtra.cer
# 2. Cho tài khoản Windows tin chứng chỉ (Windows hỏi xác nhận -> Yes)
certutil -user -addstore Root ssl\tiemtra.cer
```
3. Đóng **hết** cửa sổ Chrome rồi mở lại `https://<IP>:8443/admin`, thanh địa chỉ sẽ hiện ổ khoá.

- Đổi Wi-Fi / IP thì server tạo chứng chỉ mới, phải làm lại 2 bước trên.
- Gỡ ra: `certutil -user -delstore Root "Tiem Tra"`
- Không dùng được cách này cho máy khách. Muốn khách không thấy cảnh báo thì chạy HTTP: `APP_HTTPS=false`.

**So sánh HTTP và HTTPS trên Wireshark:**
- Chạy 1 lần **HTTP** (`$env:APP_HTTPS="false"; .\mvnw.cmd spring-boot:run` → cổng 8081), filter `tcp.port == 8081 && websocket`: đọc được nội dung tin nhắn.
- Chạy 1 lần **HTTPS** (mặc định), filter `tcp.port == 8443`: chỉ thấy gói TLS mã hoá.

Script `ssl/tao-chung-chi.bat|sh` không còn bắt buộc (server tự tạo chứng chỉ).

---

## 7. Cấu hình bằng biến môi trường

Đặt biến **trước lệnh chạy** (chỉ có tác dụng trong cửa sổ terminal đó):
```powershell
$env:APP_HTTPS="false"; .\mvnw.cmd spring-boot:run              # PowerShell
set "APP_HTTPS=false" && mvnw.cmd spring-boot:run                # CMD (giữ dấu ngoặc kép)
APP_HTTPS=false ./mvnw spring-boot:run                           # Linux / macOS
```

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/tiemtra` | Địa chỉ MongoDB và tên database |
| `APP_HTTPS` | `true` | `false` = chạy HTTP ở cổng 8081 (không cảnh báo chứng chỉ; dùng khi demo nhiều điện thoại / so sánh trên Wireshark) |
| `PORT` | `8443` (HTTP: `8081`) | Cổng chính. **Đừng đặt `PORT=8081` khi đang chạy HTTPS**: HTTPS sẽ chiếm 8081 và cổng chuyển hướng bị tắt, nên link / mã QR `http://<IP>:8081` cũ báo lỗi. Muốn đổi cổng thì đổi cả hai, vd. `PORT=8444`, `HTTP_PORT=8082` |
| `HTTP_PORT` | `8081` | Cổng HTTP chỉ để chuyển hướng sang HTTPS. `0` = tắt |
| `APP_OPEN_BROWSER` | `true` | `false` = không tự mở trình duyệt |
| `APP_DUYET_MAY_MOI` | `true` | `false` = máy thứ 2 vào bàn không cần nhân viên duyệt |
| `APP_RATE_LIMIT` | `true` | `false` = tắt giới hạn số request (khi đo tải / test tự động) |
| `APP_UPLOAD_DIR` | `./uploads` | Thư mục ảnh món |
| `SSL_PASSWORD` | `tiemtra123` | Mật khẩu file chứng chỉ |
| `APP_CORS_ORIGINS` | localhost | Origin được gọi API khi mở frontend bằng Live Server |

Các ngưỡng bảo mật (số lần đăng nhập sai, giới hạn request…) nằm trong `src/main/resources/application.properties`, nhóm `app.security.*`. Mỗi dòng đều có chú thích tiếng Việt.

---

## 8. Server tự làm gì · Dữ liệu lưu ở đâu

**Mỗi lần khởi động, server tự:**

| Việc | Chi tiết |
|---|---|
| **Nạp dữ liệu mẫu** (từng collection, khi collection đó còn trống) | Từ `db_tiemtra/`: 3 tài khoản đăng nhập, 7 loại món, 67 món (mã 1–67), 15 khuyến mãi, 1 nhân viên. **Không** nạp đơn hàng, nên sổ đơn và trang Doanh thu bắt đầu trống. Xoá hết dữ liệu một collection (vd. `nhan_vien`) thì lần chạy sau server nạp lại bản mẫu |
| **Tạo 10 bàn** (khi chưa có) | Mỗi bàn có mã QR ngẫu nhiên, riêng cho từng máy |
| **Băm mật khẩu** | Mật khẩu mẫu dạng chữ thường được băm BCrypt |
| **Tạo / làm mới chứng chỉ HTTPS** | `ssl/tiemtra.p12`, có `localhost`, `127.0.0.1`, IP Wi-Fi. Tạo lại khi đổi mạng hoặc sắp hết hạn |
| **Chuyển console sang UTF-8** (Windows) | Để log tiếng Việt không bị thành `?` |
| **Tìm IP Wi-Fi, mở trình duyệt** | Bỏ qua card mạng ảo (VMware, VPN, WSL…). Báo khi IP khác lần chạy trước |
| **Tạo thư mục** | `uploads/` (ảnh món), `logs/` (log + nhật ký bảo mật) |

Muốn **làm lại dữ liệu từ đầu**: xoá database `tiemtra` trong Compass rồi chạy lại server.
Vẫn có thể nạp dữ liệu mẫu bằng tay: `mongoimport --db tiemtra --collection <tên> --jsonArray --file db_tiemtra/tiemtra.<tên>.json`.

**Phân biệt 2 loại "người dùng":**
- Collection `admin`: **tài khoản đăng nhập** trang quản trị (`ghann`, `lhao`, `mdung`). Mật khẩu băm BCrypt.
- Collection `nhan_vien`: chỉ là **danh sách nhân viên** (họ tên, chức vụ…). Hiện code **chưa dùng** ở đâu; thêm / xoá nhân viên không ảnh hưởng đăng nhập.

**Thêm / sửa món:** trang quản trị chưa có chức năng này. Sửa trực tiếp collection `mon_an` bằng Compass:
- Mỗi món cần `idMon` (không trùng; hiện dùng 1–67, món mới dùng 68 trở đi), `TenMon`, `idLoai`, `Gia`.
- Ảnh: đặt file `uploads/<idMon>.jpg`. Không cần ghi `image`; server tự tìm theo mã món.
- Muốn máy khác chạy lần đầu cũng có món đó thì sửa thêm `db_tiemtra/tiemtra.mon_an.json`.

**Dữ liệu lưu ở đâu:**

| Dữ liệu | Lưu ở | Ghi chú |
|---|---|---|
| Đơn hàng, thực đơn, bàn, tài khoản, giỏ hàng | **MongoDB**, database `tiemtra` trên máy chạy server | **Doanh thu không lưu riêng**: trang Doanh thu tính trực tiếp từ đơn hàng (MongoDB tự cộng – aggregation) |
| Tin nhắn chat | RAM của server | Tối đa 100 tin/bàn, xoá khi trả bàn, mất khi tắt server |
| Trình duyệt (`localStorage`) | Máy người dùng | Chỉ vài thứ nhỏ: token đăng nhập, mã bàn, cài đặt âm báo… – **không** có đơn hay doanh thu |
| Log `logs/app.log` | Thư mục project | Tự cắt file 10 MB, giữ 7 ngày, tổng tối đa 100 MB |

**Dữ liệu nhiều có sao không?**
- Mỗi đơn khoảng 0,5–1 KB, nên 500 đơn/ngày × 1 năm ≈ 180.000 đơn ≈ 100 MB.
- Đã đo với 182.500 đơn: màn hình bếp ~30 ms, doanh thu 30 ngày ~0,3 giây, cả năm ~3 giây.
- **Sao lưu:** `mongodump --db tiemtra --out backup\` (khôi phục: `mongorestore backup\`), hoặc dùng *Export Collection* trong Compass.

---

## 9. Đổi Wi-Fi / chuyển sang máy khác

- Chỉ cần **chạy lại server**. IP mới được tự tìm, chứng chỉ HTTPS tự tạo lại.
- **Phải in lại mã QR**, vì mã QR chứa IP của máy chạy server. Server sẽ cảnh báo *"IP Wi-Fi đã đổi… hãy in lại mã QR"*.
- Router có thể cấp IP khác **ngay cả khi không đổi Wi-Fi**. Muốn khỏi in lại, đặt IP cố định cho máy trong router (*DHCP reservation*).
- Địa chỉ đổi thì trình duyệt coi là trang khác, nên phải **đăng nhập quản trị lại** một lần.

**Mang project sang máy khác (copy thư mục / file zip):**
- Nên dùng `git clone`, hoặc khi nén zip thì bỏ các thư mục `target/`, `logs/`, `ssl/*.p12` (giữ `uploads/` vì là ảnh món). Máy mới tự tạo lại những thứ đã bỏ.
- Lỡ copy cả `logs/` thì vẫn chạy bình thường. Server lưu IP **kèm tên máy** ở `logs/ip-lan.txt`, nên máy mới **không** báo nhầm *"IP Wi-Fi đã đổi"*. Cảnh báo chỉ hiện khi **cùng một máy** đổi IP.
- Dữ liệu (đơn, bàn, tài khoản) nằm trong MongoDB của từng máy, không đi theo thư mục project. Máy mới bắt đầu bằng dữ liệu mẫu. Muốn mang dữ liệu theo: `mongodump` / `mongorestore` (mục 8).
- **Chữ tiếng Việt trong terminal:** console Windows trên nhiều máy để bảng mã cũ (437 / 1252), trước đây log hiện thành `Ti?m Trà ?? ch?y`. Nay server **tự chuyển console sang UTF-8** (`chcp 65001`) lúc khởi động, nên máy nào cũng hiện đúng. Cửa sổ terminal đó giữ UTF-8 sau khi tắt server; việc này vô hại.

**Làm việc nhóm với Git** (repo: https://github.com/phamminhdung101-stack/Nhom4, nhánh `master`):
```powershell
git pull                          # lấy code mới nhất của nhóm (làm trước khi sửa)
git add -A
git commit -m "mô tả thay đổi"
git push                          # đẩy lên; bị từ chối thì git pull rồi push lại
```
- Code frontend nằm ở `src/main/resources/static/`, **không** sửa các thư mục cũ ở gốc repo (đã bỏ).
- **Ảnh món có lên git** (`uploads/`): thêm / đổi ảnh thì `git add` cả file ảnh. **Không lên git:** chứng chỉ (`ssl/*.p12`), log, `target/`.
- **Dữ liệu (đơn, bàn…) không lên git:** mỗi máy có MongoDB riêng. Đổi dữ liệu mẫu thì sửa file trong `db_tiemtra/`; máy khác phải xoá database `tiemtra` rồi chạy lại mới nhận bản mẫu mới.

---

## 10. Luồng nghiệp vụ

1. **Khách quét QR** → `welcome.html` xin vào bàn.
   - Máy **đầu tiên** ở bàn trống là *chủ bàn*, gọi món được ngay, bàn chuyển *Có khách*.
   - Máy **thứ 2 trở đi** (bạn cùng bàn, hoặc người lạ quét trộm QR) phải chờ **nhân viên bấm "Cho phép"** ở Sơ đồ bàn (có âm báo + toast). Trong lúc chờ không thêm món / đặt / chat / xem đơn của bàn được.
   - Nhân viên có thể *Từ chối* hoặc *Mời ra* bất kỳ máy nào.
2. **Chọn món**, size, độ ngọt, topping → **giỏ hàng chung của bàn**, lưu trên server. Nhiều máy cùng bàn dùng chung giỏ, cập nhật realtime. **Giá do server tính**.
3. **Gửi bếp** → server lấy và xoá giỏ trong 1 thao tác (bấm 2 lần hay 2 máy bấm cùng lúc cũng chỉ ra 1 đơn), tạo đơn với mã tăng dần. Tạo đơn lỗi thì giỏ được trả lại.
4. Bếp / thu ngân nhận `NEW_ORDER` tức thì, có **âm báo**. Bếp chuyển trạng thái: **0 Chờ xác nhận → 1 Đang pha chế → 2 Đang mang ra → 3 Hoàn thành**.
5. Khách theo dõi realtime ở `pages/tracking.html` (chỉ nhận sự kiện của bàn mình); chat / gọi phục vụ với nhân viên. Khách nhắn tin → mọi trang quản trị đều có toast + âm báo + số tin chưa đọc.
6. **Huỷ đơn** (trạng thái 4): khách tự huỷ khi đơn còn *Chờ xác nhận*. Bếp đã nhận đơn thì khách bấm *Nhắn quán xin huỷ*, nhân viên bấm *Huỷ* ở màn hình bếp hoặc sơ đồ bàn. Đơn huỷ không tính doanh thu.
7. Khách **nhận đủ món** (mọi đơn của lượt khách đã *Hoàn thành* / huỷ, có ít nhất 1 đơn hoàn thành, giỏ trống) → bàn **tự về Trống**, phiên chat bị xoá.
   - Phiên bàn kết thúc: máy của khách cũ không đặt món từ xa được nữa, nhưng vẫn xem lại được đơn của mình.
   - Nút *Hoàn thành* / *Reset bàn* để trả bàn bằng tay.

---

## 11. Bảo mật đã áp dụng

| Lớp | Cách làm | Code |
|---|---|---|
| Xác thực nhân viên | Đăng nhập → token ngẫu nhiên 12h (header `X-Admin-Token`), đăng xuất thu hồi token | `AuthService`, `AdminService` |
| Mật khẩu | BCrypt; dữ liệu cũ tự băm; tài khoản không tồn tại vẫn chạy BCrypt giả (chống đoán tài khoản qua thời gian phản hồi) | `AdminService` |
| Chống dò mật khẩu | Sai 10 lần (cùng tài khoản + IP) → khoá 1 phút; 1 IP sai 30 lần → khoá IP 5 phút; báo số lần thử còn lại | `LoginAttemptService` |
| Phân quyền theo bàn | Mã in trong QR chỉ để **xin vào bàn**; mỗi điện thoại nhận mã riêng (header `X-Table-Token`). Máy thứ 2 trở đi phải được nhân viên cho phép. Bàn trả về Trống → mã của các máy hết hiệu lực. Tối đa 10 máy chờ duyệt/bàn. Có thể đổi mã QR | `AuthService`, `BanAnService`, `ThietBi` |
| Thao tác đồng thời | Đổi trạng thái đơn, gửi bếp, sửa giỏ chung đều nguyên tử / khoá lạc quan: nhiều người bấm cùng lúc không mất món, không ra đơn trùng | `DonHangService`, `GioHangService` |
| Giới hạn tần suất | Theo IP: 1000 request/phút, 300 thao tác ghi/phút, 20 lần đăng nhập/phút → trả `429` + `Retry-After`. Tắt khi đo tải: `APP_RATE_LIMIT=false` | `RateLimitFilter` |
| Giới hạn kích thước | Body API ≤ 1 MB (413), tin WebSocket/STOMP ≤ 16 KB | `RateLimitFilter`, `WebSocketConfig`, `WebSocketChatConfig` |
| Chống spam WebSocket | `/ws`: > 30 tin / 10s → đóng kết nối; chat: > 10 tin / 10s → bỏ qua | `OrderWebSocketHandler`, `ChatController` |
| Header bảo mật | Content-Security-Policy, X-Frame-Options (chống clickjacking), nosniff, Referrer-Policy, Permissions-Policy, `Cache-Control: no-store` cho API; HSTS mặc định tắt (`app.security.hsts`) | `SecurityHeadersFilter` |
| Chống XSS | Client hiển thị bằng `textContent`/escape; CSP chặn script lạ | `app.js`, `SecurityHeadersFilter` |
| Chống sửa giá | Giá luôn do server tính từ DB | `MonAnService` |
| Không lộ lỗi nội bộ | Lỗi trả JSON gọn, không stacktrace | `GlobalExceptionHandler`, `application.properties` |
| CORS / Origin | Chỉ các origin khai báo trong `app.cors.allowed-origins` | `WebConfig`, `WebSocketConfig` |
| Nhật ký bảo mật | Đăng nhập sai/khoá, mã bàn sai, truy cập trái phép, spam… ghi vào `logs/app.log` (logger `AUDIT`) | `AuditLog` |
| HTTPS / WSS (mặc định) | TLS 1.3, chứng chỉ tự ký tự tạo khi khởi động (có localhost, 127.0.0.1, IP Wi-Fi), tự làm mới khi đổi mạng / sắp hết hạn. Cổng HTTP 8081 chỉ chuyển hướng sang HTTPS 8443 (API không trả dữ liệu qua HTTP). Tắt: `APP_HTTPS=false` | `ChungChiHttps`, `HttpsConfig` |

> **Mức bảo mật cho đồ án:** các ngưỡng trên đã được nới để demo trên lớp không bị chặn (1 máy mở nhiều tab bếp/POS/sơ đồ bàn, lỡ gõ sai mật khẩu vài lần). Mọi ngưỡng nằm trong `application.properties` (nhóm `app.security.*`). Muốn chặt hơn khi chạy thật thì giảm số lần thử / tăng thời gian khoá ở đó, không cần sửa code.

---

## 12. API REST

Quyền: **NV** = nhân viên (header `X-Admin-Token`); **bàn** = nhân viên hoặc máy đã được duyệt ở bàn đó (header `X-Table-Token`); **máy** = máy đã vào bàn (kể cả đang chờ duyệt).

| Method | Đường dẫn | Quyền | Ghi chú |
|---|---|---|---|
| GET | `/api/mon-an`, `/api/mon-an/loai/{idLoai}` | công khai | thực đơn |
| GET | `/api/mon-an/tuy-chon` | công khai | size, topping, độ ngọt + giá |
| POST | `/api/ban-an/{idBan}/vao-ban` | – | `{qr}` (mã in trong QR) → `{token, id, ten, laChu, trangThai: DA_DUYET\|CHO_DUYET}` |
| GET | `/api/ban-an/{idBan}/thiet-bi/toi` | máy | trạng thái máy này (máy chờ duyệt hỏi mỗi 3 giây; 403 = bị từ chối / phiên đã kết thúc) |
| POST | `/api/ban-an/{idBan}/thiet-bi/{id}/duyet` | NV | cho phép máy vào bàn |
| DELETE | `/api/ban-an/{idBan}/thiet-bi/{id}` | NV | từ chối / mời máy ra khỏi bàn |
| GET | `/api/ban-an/{idBan}/xac-thuc` | bàn | thông tin bàn |
| PUT | `/api/ban-an/{idBan}/co-khach` | bàn | đánh dấu có khách |
| GET | `/api/gio-hang/{idBan}` | bàn | giỏ hàng |
| POST | `/api/gio-hang/{idBan}/items` | bàn | `{idMon, soLuong, size, doNgot, toppings[], ghiChu}` |
| PATCH | `/api/gio-hang/{idBan}/items/{itemId}` | bàn | `{soLuong}` (≤ 0 = xoá) |
| DELETE | `/api/gio-hang/{idBan}/items/{itemId}` | bàn | |
| POST | `/api/don-hang/dat-mon/{idBan}` | bàn | `{ghiChu}` → tạo đơn từ giỏ |
| GET | `/api/don-hang/ban/{idBan}` | bàn | đơn mới nhất (204 nếu chưa có) |
| GET | `/api/don-hang/ban/{idBan}/luot-nay` | bàn | mọi đơn của lượt khách hiện tại (mới nhất trước) |
| GET | `/api/chat/history/{idBan}` | bàn | lịch sử chat phiên hiện tại |
| POST | `/api/chat/{idBan}` | máy | `{content, clientId}` – gửi chat qua HTTP (dự phòng khi WebSocket bị chặn) |
| POST | `/api/admin/login` | – | form `username`, `matKhau` → `{token,...}` |
| POST | `/api/admin/logout`, GET `/api/admin/me` | NV | |
| GET | `/api/don-hang?trangThai=&tu=` | NV | danh sách đơn cho màn hình bếp: mặc định đơn **hôm nay** + đơn chưa xong 2 ngày gần nhất; `tu=YYYY-MM-DD` để xem từ ngày khác; tối đa 500 đơn |
| PATCH | `/api/don-hang/{id}/trang-thai` | NV | `{trangThai: 0..3}` |
| POST | `/api/don-hang/{id}/huy` | bàn / NV | `{lyDo}` – khách chỉ huỷ được đơn *Chờ xác nhận* |
| POST | `/api/don-hang/tai-quay` | NV | đơn tại quầy `{items:[{idMon,soLuong}], ghiChu}` |
| GET | `/api/don-hang/thong-ke` | NV | số liệu tóm tắt (7 ngày) |
| GET | `/api/don-hang/bao-cao?tu=YYYY-MM-DD&den=YYYY-MM-DD` | NV | báo cáo doanh thu chi tiết (theo ngày, giờ, kênh, món, bàn, chi tiết đơn) |
| GET | `/api/ban-an` | NV | danh sách bàn + link QR |
| PUT | `/api/ban-an/{idBan}/trang-thai/{0\|1}` | NV | |
| PUT | `/api/ban-an/reset` | NV | `{idBans:[1,3]}` → chỉ reset các bàn đó; không có body → reset toàn bộ |
| POST/GET | `/api/ban-an/{idBan}/qr-image` | NV | lưu / tải ảnh QR |
| POST | `/api/ban-an/{idBan}/doi-ma-qr` | NV | cấp mã QR mới |

Lỗi luôn trả JSON: `{"status": 403, "message": "...", "time": "..."}`.

---

## 13. Giao thức WebSocket

### 13.1 Raw WebSocket `wss://<host>:8443/ws` – thông báo đơn (chạy HTTP: `ws://<host>:8081/ws`)

Client gửi (text frame JSON) ngay sau khi kết nối:

```json
{"type":"JOIN_ROOM","room":"KITCHEN","token":"<token nhân viên>"}     // hoặc room "CASHIER"
{"type":"JOIN_TABLE","idBan":1,"token":"<mã riêng của máy (X-Table-Token)>"}
```

Mã in trên QR **không** dùng được để vào phòng bàn. Server trả `{"type":"JOINED","room":"..."}` hoặc `{"type":"ERROR","message":"..."}`, sau đó đẩy sự kiện:

```json
{"type":"NEW_ORDER","orderId":"...","idDonHang":25,"tableId":3,"status":0,"tongTien":95000,"soMon":2}
{"type":"ORDER_STATUS_UPDATED","orderId":"...","idDonHang":25,"tableId":3,"status":1,...}
{"type":"TABLE_STATUS_UPDATED","idBan":3,"trangThai":0,"tuDong":true}
{"type":"CART_UPDATED","idBan":3,"tongSoLuong":2,"tongTien":60000}
{"type":"DEVICE_PENDING","idBan":3,"tenBan":"Bàn 03","may":"Máy 2"}
{"type":"LEFT_TABLE","idBan":3}
```

| Sự kiện | Gửi tới | Khi nào |
|---|---|---|
| `NEW_ORDER` | KITCHEN, CASHIER, `TABLE_{idBan}` | Có đơn mới (tại bàn hoặc tại quầy) |
| `ORDER_STATUS_UPDATED` | KITCHEN, CASHIER, `TABLE_{idBan}` | Đơn đổi trạng thái / bị huỷ (kèm `lyDoHuy`, `nguoiHuy`) |
| `TABLE_STATUS_UPDATED` | KITCHEN, CASHIER | Bàn có khách / trống (`tuDong: true` = tự trả bàn); `idBan: 0` = reset toàn bộ |
| `CART_UPDATED` | `TABLE_{idBan}` | Giỏ chung đổi (máy khác cùng bàn thêm / xoá món hoặc gửi bếp) |
| `DEVICE_PENDING` | KITCHEN, CASHIER | Có máy mới xin vào bàn đang có khách |
| `LEFT_TABLE` | riêng kết nối bị loại | Máy bị mời ra / phiên bàn kết thúc |

**Heartbeat:** server gửi **Ping frame (opcode 0x9)** mỗi 25 giây, client trả **Pong (0xA)** (trình duyệt tự trả). Kết nối không phản hồi quá 60 giây bị đóng. Vẫn hỗ trợ text `"ping"` → `"pong"` cho client đơn giản.

**Client desktop (C#/Python):** gọi `POST /api/admin/login` lấy `token` → mở `wss://<host>:8443/ws` (chứng chỉ tự ký: client cần bỏ qua kiểm tra chứng chỉ) → gửi `JOIN_ROOM` với room `CASHIER`.

### 13.2 STOMP `wss://<host>:8443/ws-chat` – chat bàn ↔ nhân viên

- CONNECT header: khách `role=CUSTOMER, idBan, tableToken` (mã riêng của máy); nhân viên `role=STAFF, adminToken`.
- Khách SUBSCRIBE `/topic/chat/table/{idBan}`; nhân viên `/topic/chat/staff` và `/topic/chat/table/{n}`.
- Gửi: SEND `/app/chat.send` body `{"content":"...","idBan":n}` (khách không cần `idBan`, server dùng bàn đã xác thực).
- STOMP heart-beat 10s/10s.

---

## 14. Cấu trúc mã nguồn

```
src/main/java/chandanv/local/chandanv
├── ChandanvApplication.java   điểm chạy (main) – chuẩn bị console + chứng chỉ HTTPS rồi khởi động Spring
├── config/        WebConfig (CORS, ảnh), WebSocketConfig, WebSocketChatConfig, HttpsConfig,
│                  ChungChiHttps (tự tạo chứng chỉ), ConsoleUtf8 (tiếng Việt trên console Windows),
│                  DataInitializer (nạp dữ liệu mẫu, tạo bàn),
│                  LanBrowserLauncher (tìm IP Wi-Fi, mở trình duyệt), GlobalExceptionHandler, MongoFailureAnalyzer
├── controllers/   REST API + STOMP chat
├── services/      nghiệp vụ: đơn hàng & doanh thu, giỏ hàng, bàn/phiên bàn, thực đơn & giá, xác thực, chat
├── models/        entity (MongoDB) và dto
├── repositories/  Spring Data MongoDB
├── security/      giới hạn request, khoá đăng nhập, header bảo mật, nhật ký bảo mật (AuditLog)
├── websocket/     OrderWebSocketHandler (/ws: phòng bếp/thu ngân/bàn, ping/pong), sự kiện, bảo mật chat
└── util/
src/main/resources
├── application.properties     cấu hình (có chú thích)
└── static/                    frontend – khách: welcome.html, index.html, pages/ ; quản trị: admin/
db_tiemtra/                    dữ liệu mẫu (JSON) – tự nạp khi DB trống
uploads/                       ảnh món, tên = mã món (1.jpg … 67.jpg) – có trên git
ssl/                           chứng chỉ HTTPS (tự tạo, KHÔNG đưa lên git)
```

**Khi sửa code:**
- Chạy bằng `spring-boot:run` thì server **tự khởi động lại** khi biên dịch lại (DevTools).
- Sửa HTML/JS xong thì bấm **Ctrl + F5** trên trình duyệt.

---

## 15. Gặp lỗi thì làm gì

| Hiện tượng | Cách xử lý |
|---|---|
| `mvnw.cmd is not recognized` | PowerShell phải gõ `.\mvnw.cmd ...` |
| `Error: JAVA_HOME not found in your environment` / `'java' is not recognized` | Chưa cài JDK, hoặc chưa có `JAVA_HOME`. Cài lại JDK và bật *Set JAVA_HOME* (mục 1.1), **mở terminal mới** rồi chạy lại |
| `invalid target release: 17` / lỗi phiên bản Java | Máy đang dùng JDK cũ hơn 17. Kiểm tra `java -version` và `JAVA_HOME` |
| Lần đầu báo lỗi tải thư viện (`Could not transfer artifact`…) | Lần đầu cần Internet. Kiểm tra mạng / VPN / proxy rồi chạy lại |
| **APPLICATION FAILED TO START** – *Không kết nối được MongoDB* | Chưa cài hoặc chưa bật MongoDB. `Get-Service MongoDB` phải là `Running` (mục 1.2), hoặc đặt `MONGODB_URI` |
| `Port 8443 (hoặc 8081) was already in use` | Còn server khác đang chạy (thường là lần chạy trước chưa tắt, xem mục 16). Hoặc đổi cổng: `$env:PORT="8444"; $env:HTTP_PORT="8082"` |
| Trình duyệt báo *"Kết nối không riêng tư"* | Bình thường với chứng chỉ tự ký: *Nâng cao → Tiếp tục*. Muốn tránh thì chạy `APP_HTTPS=false` |
| Vào được rồi nhưng thanh địa chỉ vẫn **"Not secure"**, `https` **bị gạch** | Bình thường, vẫn mã hoá. Muốn có ổ khoá trên máy quản trị: mục 6 |
| Log ghi `Tomcat started on port 8081 (https)`; link `http://<IP>:8081` báo lỗi | Terminal đang có biến `PORT=8081`. Tắt server (`Ctrl+C`), kiểm tra `echo $env:PORT $env:SERVER_PORT`, xoá bằng `Remove-Item Env:PORT, Env:SERVER_PORT -ErrorAction SilentlyContinue` (hoặc mở terminal mới), rồi chạy lại |
| Điện thoại không mở được trang | Cùng Wi-Fi chưa? Có đang dùng IP (không phải `localhost`) không? Đã cho Java qua Windows Firewall chưa? |
| Khách quét QR không vào được sau khi đổi Wi-Fi | Mã QR chứa IP cũ, phải in lại ở trang *Mã QR bàn* |
| Sửa code mà trang không đổi | Bấm **Ctrl + F5** để tải lại không dùng cache |
| Báo `429` / *"thao tác quá nhanh"* / *"tài khoản bị khoá 1 phút"* | Tính năng bảo mật đang chạy đúng. Chờ một chút, hoặc tắt khi test: `APP_RATE_LIMIT=false` |
| Máy thứ 2 quét QR bị *"Chờ nhân viên xác nhận"* | Vào *Sơ đồ bàn* → chọn bàn → **Cho phép**. Hoặc chạy `APP_DUYET_MAY_MOI=false` |
| Không nghe âm báo đơn mới | Trình duyệt chặn âm thanh khi chưa bấm vào trang. Bấm vào trang một lần (hoặc nút đỏ *"bấm để bật âm báo"*). Kiểm tra *Âm báo: Bật* ở sidebar |
| Chữ tiếng Việt trong terminal thành `?` (vd. `Ti?m Trà ?? ch?y`) | Server đã tự chuyển console sang UTF-8. Nếu vẫn thấy: đang chạy bản code cũ (lấy bản mới nhất), hoặc font của terminal thiếu chữ tiếng Việt (đổi sang *Consolas* / *Cascadia Mono*). Cách tay: gõ `chcp 65001` rồi chạy lại. Log trong `logs/app.log` luôn đúng UTF-8 |
| Máy mới vừa copy project về đã báo *"IP Wi-Fi đã đổi"* | Do `logs/ip-lan.txt` của máy cũ đi theo, và máy cũ chạy bản code trước bản lưu tên máy. Bỏ qua được (máy mới chưa in mã QR nào), hoặc xoá thư mục `logs/`. Từ lần chạy sau không báo nữa |
| File `.md` mở bằng Notepad cũ bị lỗi chữ (`Ä‘`, `á»`…) | File lưu UTF-8. Mở bằng VS Code, Notepad của Windows 10 (bản 1903 trở lên) / Windows 11, hoặc xem trên GitHub |
| Màn hình bếp không thấy đơn hôm qua | Đúng thiết kế: bếp chỉ hiện đơn hôm nay + đơn chưa xong 2 ngày gần nhất. Đơn cũ xem ở trang *Doanh thu* |
| Trang Doanh thu trống | Máy mới chưa có đơn nào. Làm thử mục 3 |
| Món không có ảnh (hiện ảnh mặc định) | Kiểm tra `uploads/` có file của món đó không (chạy `git pull` để lấy ảnh mới nhất). Tên file phải đúng mã món (`12.jpg`; nhận `.jpg` `.jpeg` `.png` `.webp`). Chép xong bấm **Ctrl + F5**, không cần chạy lại server |
| Chữ trên web trông khác máy khác (font mặc định) | Font Be Vietnam Pro tải từ Google: máy không có Internet sẽ dùng font hệ thống. Web vẫn chạy bình thường |
| Bàn hiện số tin chưa đọc nhưng mở ra không có tin | Số lưu trong trình duyệt; mở lại trang *Sơ đồ bàn* là tự dọn số của bàn đã Trống. Bàn đang có khách thì bấm vào bàn đó để đánh dấu đã đọc |

---

## 16. Tắt server · Dọn dẹp

- **Tắt server:** bấm `Ctrl + C` trong terminal đang chạy (hoặc nút ■ Stop trong IDE).
- **Kiểm tra còn server nào giữ cổng không** (khi gặp lỗi *Port already in use*):
  ```powershell
  Get-NetTCPConnection -LocalPort 8443,8081 -State Listen -ErrorAction SilentlyContinue | Select-Object LocalPort, OwningProcess
  Stop-Process -Id <OwningProcess> -Force     # tắt tiến trình đang giữ cổng
  ```
- **Xoá dữ liệu để làm lại từ đầu:** xoá database `tiemtra` trong Compass, rồi chạy lại server (dữ liệu mẫu tự nạp lại).
- **Gỡ chứng chỉ đã cho Windows tin** (nếu đã làm ở mục 6): `certutil -user -delstore Root "Tiem Tra"`
