package chandanv.local.chandanv.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.repositories.BanAnRepository;

@Configuration
public class BanAnDataInit implements CommandLineRunner {

    private final BanAnRepository repo;

    public BanAnDataInit(BanAnRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {

        if (repo.count() == 0) {
            for (int i = 1; i <= 10; i++) {

                BanAn ban = new BanAn();
                ban.setIdBan(i);
                ban.setTenBan("Bàn " + (i < 10 ? "0" + i : i));
                ban.setTrangThai(0);
                ban.setMaQR("http://localhost:8080/index.html?idBan=" + i);

                repo.save(ban);
            }

            System.out.println("Đã tạo sẵn 10 bàn (1–10)");
        }
    }
}