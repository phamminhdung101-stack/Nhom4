package chandanv.local.chandanv.websocket;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest req = servletRequest.getServletRequest();
            String idBan = req.getParameter("idBan");
            String role = req.getParameter("role");
            String adminId = req.getParameter("adminId");
            String sender = req.getParameter("sender");

            if (idBan != null && !idBan.isBlank()) {
                attributes.put("idBan", idBan.trim());
            }
            if (role != null && !role.isBlank()) {
                attributes.put("role", role.trim());
            }
            if (adminId != null && !adminId.isBlank()) {
                attributes.put("adminId", adminId.trim());
            }
            if (sender != null && !sender.isBlank()) {
                attributes.put("sender", sender.trim());
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}

