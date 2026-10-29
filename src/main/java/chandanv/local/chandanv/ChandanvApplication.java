package chandanv.local.chandanv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import chandanv.local.chandanv.config.ChungChiHttps;
import chandanv.local.chandanv.config.ConsoleUtf8;

@SpringBootApplication
@EnableScheduling // cần cho heartbeat Ping/Pong của WebSocket
public class ChandanvApplication {

    public static void main(String[] args) {
        ConsoleUtf8.bat(); // console Windows hiện đúng tiếng Việt (không thì log ra "Ti?m Trà ?? ch?y")
        ChungChiHttps.chuanBi(); // tự tạo / làm mới chứng chỉ HTTPS theo IP Wi-Fi trước khi server mở cổng
        SpringApplication.run(ChandanvApplication.class, args);
    }
}
