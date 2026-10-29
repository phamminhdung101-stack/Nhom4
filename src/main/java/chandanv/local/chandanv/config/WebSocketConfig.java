package chandanv.local.chandanv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import chandanv.local.chandanv.websocket.OrderWebSocketHandler;

/** Raw WebSocket cho thông báo đơn hàng: wss://host:8443/ws (chạy HTTP: ws://host:8081/ws). */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderWebSocketHandler orderWebSocketHandler;
    private final String[] allowedOrigins;

    public WebSocketConfig(OrderWebSocketHandler orderWebSocketHandler,
                           @Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.orderWebSocketHandler = orderWebSocketHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Cùng origin (trang do chính server phục vụ, kể cả qua IP LAN) luôn được chấp nhận;
        // danh sách allowedOrigins chỉ dành cho môi trường dev (Live Server).
        registry.addHandler(orderWebSocketHandler, "/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    /** Giới hạn kích thước 1 tin nhắn WebSocket (chống gửi gói khổng lồ làm tràn bộ nhớ). */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(16 * 1024);
        container.setMaxBinaryMessageBufferSize(16 * 1024);
        return container;
    }
}
