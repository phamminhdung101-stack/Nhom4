package chandanv.local.chandanv.config;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Khi chạy HTTPS: mở thêm cổng HTTP (mặc định 8081) chỉ để chuyển hướng sang HTTPS
 * -> gõ http://<IP>:8081 hoặc quét mã QR in từ trước (trỏ http) vẫn vào được, tự sang https://<IP>:8443.
 */
@Configuration
public class HttpsConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> chuyenHttpSangHttps(
            @Value("${server.ssl.enabled:false}") boolean ssl,
            @Value("${app.http-port:8081}") int httpPort,
            @Value("${server.port:8443}") int httpsPort) {
        return factory -> {
            if (!ssl || httpPort <= 0 || httpPort == httpsPort) return;
            // Mọi đường dẫn đều yêu cầu kênh bảo mật -> request vào cổng HTTP bị chuyển sang cổng HTTPS (giữ nguyên đường dẫn)
            factory.addContextCustomizers(context -> {
                SecurityConstraint rangBuoc = new SecurityConstraint();
                rangBuoc.setUserConstraint("CONFIDENTIAL");
                SecurityCollection tatCa = new SecurityCollection();
                tatCa.addPattern("/*");
                rangBuoc.addCollection(tatCa);
                context.addConstraint(rangBuoc);
            });
            Connector http = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            http.setScheme("http");
            http.setPort(httpPort);
            http.setSecure(false);
            http.setRedirectPort(httpsPort);
            factory.addAdditionalTomcatConnectors(http);
        };
    }
}
