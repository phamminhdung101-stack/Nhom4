Đề tài - tiểu luận: Nghiên cứu và xây dựng hệ thống giao tiếp thời gian thực cho hệ thống Order món ăn qua mã QR sử dụng WebSocket.

Nhóm công nghệ chính:
-TCP/IP
-HTTP/HTTPS
-WebSocket
-HTML/CSS
-Java (framework Spring Boot)
-JavaScript
-Database (MongoDB)
-Wireshark

1. Mục tiêu

-Phát triển hệ thống Order món ăn qua mã QR từ hệ thống hiện có, đồng thời ứng dụng kỹ thuật lập trình mạng WebSocket để xây dựng khả năng giao tiếp và cập nhật dữ liệu theo thời gian thực giữa khách hàng, bếp và thu ngân.

2. Nội dung chính
-Kế thừa hệ thống Web Order món ăn qua QR.
-Xây dựng giao tiếp Client – Server bằng Raw WebSocket.
-Thông báo đơn hàng Real-time đến Bếp/Thu ngân mà không cần Refresh.
-Xây dựng Room/Channel Routing để định tuyến dữ liệu đến đúng Client.
-Xây dựng Ping/Pong Heartbeat để kiểm tra và xử lý Client mất kết nối.
-Phát triển thêm Client Desktop bằng C# hoặc Python để nhận thông báo qua WebSocket.
-Nghiên cứu mã hóa dữ liệu bằng AES và bảo mật kết nối bằng WSS/TLS.
-Sử dụng Wireshark để phân tích quá trình giao tiếp mạng.

3. Kết quả dự kiến
-Hệ thống Order món ăn bằng QR.
-WebSocket Server giao tiếp Real-time.
-Giao diện Bếp/Thu ngân nhận thông báo tức thời.
-Cơ chế Routing và Heartbeat.
-Client Desktop kết nối WebSocket.
-Bộ minh chứng Wireshark: TCP Handshake, HTTP Upgrade, WebSocket Frame, Ping/Pong và TCP Termination.
-Báo cáo và source code dự án.

Đề tài - giao thức: DNS (Domain Name System)

1. Tổng quan
-DNS (Domain Name System) là hệ thống phân giải tên miền thành địa chỉ IP, giúp các thiết bị có thể truy cập dịch vụ -mạng thông qua tên miền thay vì phải nhớ địa chỉ IP.
-Nhóm: DNS / Tên miền
-OSI: Layer 7 – Application
-Transport: UDP/TCP
-Port: 53

2. Mục tiêu
-Tìm hiểu nguyên lý hoạt động của DNS.
-Nghiên cứu quá trình DNS Query/Response.
-Tìm hiểu các loại DNS Record.
-Xây dựng mô hình DNS Server thử nghiệm.
-Phân tích hoạt động DNS bằng Wireshark.
-Nghiên cứu DNS Cache và cơ chế phân giải tên miền.

3. Nội dung chính
-DNS Client – DNS Server.
-Forward DNS và Reverse DNS.
-DNS Query/Response.
-DNS Record: A, AAAA, CNAME, MX, NS, TXT, PTR.
-DNS Cache.
-DNS Forwarding.
-Phân tích gói tin DNS.
-Nghiên cứu các vấn đề bảo mật DNS cơ bản.

4. Công nghệ / tài liệu tham khảo
-CoreDNS: https://github.com/coredns/coredns
-miekg/dns: https://github.com/miekg/dns
-Wireshark.
