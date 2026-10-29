package chandanv.local.chandanv.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final String uploadDir;

    public WebConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins,
                     @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.allowedOrigins = allowedOrigins;
        this.uploadDir = uploadDir;
    }

    /**
     * CORS: chỉ cần khi mở frontend bằng Live Server (cổng khác server) mà vẫn gọi API.
     * Trang do chính server phục vụ là cùng origin nên không bị ảnh hưởng. Danh sách origin: app.cors.allowed-origins.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    /** Ảnh món: /uploads/** đọc từ thư mục app.upload-dir (mặc định ./uploads, tương đối với thư mục chạy server). */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = path.toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(86400);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/welcome.html");
        registry.addRedirectViewController("/admin", "/admin/login.html");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
