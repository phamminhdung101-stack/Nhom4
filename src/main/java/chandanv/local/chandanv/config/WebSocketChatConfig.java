package chandanv.local.chandanv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import chandanv.local.chandanv.websocket.ChatSecurityChannelInterceptor;

/** STOMP over WebSocket cho chat bàn <-> nhân viên: wss://host:8443/ws-chat (chạy HTTP: ws://host:8081/ws-chat). */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketChatConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatSecurityChannelInterceptor securityChannelInterceptor;
    private final String[] allowedOrigins;

    public WebSocketChatConfig(ChatSecurityChannelInterceptor securityChannelInterceptor,
                               @Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.securityChannelInterceptor = securityChannelInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("stomp-heartbeat-");
        heartbeatScheduler.initialize();

        // STOMP heart-beat 10s/10s để phát hiện client mất kết nối
        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[] {10_000, 10_000})
                .setTaskScheduler(heartbeatScheduler);
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat").setAllowedOriginPatterns(allowedOrigins);
    }

    /** Giới hạn kích thước tin STOMP và bộ đệm gửi cho mỗi kết nối. */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(16 * 1024)
                .setSendBufferSizeLimit(512 * 1024)
                .setSendTimeLimit(10_000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(securityChannelInterceptor);
    }

    /** Lọc tin gửi tới khách: máy bị mời ra / hết phiên bàn không đọc được chat của bàn qua kết nối cũ. */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                return securityChannelInterceptor.locTinGuiDi(message);
            }
        });
    }
}
