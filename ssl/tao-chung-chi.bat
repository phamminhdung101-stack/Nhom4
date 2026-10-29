@echo off
REM KHONG CAN CHAY FILE NAY: server tu tao / lam moi ssl\tiemtra.p12 moi lan khoi dong (ChungChiHttps.java).
REM Chi giu lai de tham khao, hoac tao chung chi bang tay (can keytool trong PATH).
REM Tao chung chi tu ky cho HTTPS/WSS (chi dung de hoc tap / demo).
REM Chay tai thu muc goc project: ssl\tao-chung-chi.bat 192.168.1.10   (thay bang IP LAN cua may, bo trong neu chi chay local)
set IP=%1
if "%IP%"=="" set IP=127.0.0.1
keytool -genkeypair -alias tiemtra -keyalg RSA -keysize 2048 -validity 365 ^
  -storetype PKCS12 -keystore ssl\tiemtra.p12 -storepass tiemtra123 ^
  -dname "CN=Tiem Tra, OU=Nhom4, O=Nhom4, C=VN" ^
  -ext "SAN=dns:localhost,ip:127.0.0.1,ip:%IP%"
if errorlevel 1 exit /b 1
echo Da tao ssl\tiemtra.p12  ^(mat khau: tiemtra123^)
