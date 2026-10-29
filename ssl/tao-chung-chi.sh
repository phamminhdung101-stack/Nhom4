#!/usr/bin/env sh
# KHÔNG CẦN CHẠY FILE NÀY: server tự tạo / làm mới ssl/tiemtra.p12 mỗi lần khởi động (ChungChiHttps.java).
# Chỉ giữ lại để tham khảo, hoặc tạo chứng chỉ bằng tay (cần keytool trong PATH).
# Tạo chứng chỉ tự ký cho HTTPS/WSS (chỉ dùng để học tập / demo).
# Chạy tại thư mục gốc project: sh ssl/tao-chung-chi.sh 192.168.1.10   (IP LAN của máy, bỏ trống nếu chỉ chạy local)
IP=${1:-127.0.0.1}
keytool -genkeypair -alias tiemtra -keyalg RSA -keysize 2048 -validity 365 \
  -storetype PKCS12 -keystore ssl/tiemtra.p12 -storepass tiemtra123 \
  -dname "CN=Tiem Tra, OU=Nhom4, O=Nhom4, C=VN" \
  -ext "SAN=dns:localhost,ip:127.0.0.1,ip:$IP" && \
echo "Đã tạo ssl/tiemtra.p12 (mật khẩu: tiemtra123)"
