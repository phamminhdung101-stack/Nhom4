package chandanv.local.chandanv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class anhconfig implements WebMvcConfigurer {

    private static final String UPLOAD_DIR = "file:/D:/Anh_do_an/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(UPLOAD_DIR)
                .setCachePeriod(3600);
    }
}