package chandanv.local.chandanv.websocket;

import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.repositories.AdminRepository;
import chandanv.local.chandanv.repositories.BanAnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
@Component
public class ChatSecurityChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private BanAnRepository banAnRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        String role = accessor.getFirstNativeHeader("role");
        if (role == null && sessionAttributes != null) {
            role = (String) sessionAttributes.get("role");
        }

        String idBanStr = accessor.getFirstNativeHeader("idBan");
        if (idBanStr == null && sessionAttributes != null && sessionAttributes.get("idBan") != null) {
            idBanStr = String.valueOf(sessionAttributes.get("idBan"));
        }

        String adminId = accessor.getFirstNativeHeader("adminId");
        if (adminId == null && sessionAttributes != null) {
            adminId = (String) sessionAttributes.get("adminId");
        }

        String sender = accessor.getFirstNativeHeader("sender");

        if ("STAFF".equalsIgnoreCase(role)) {
            // Verify staff using AdminRepository
            Optional<Admin> admin = Optional.empty();
            if (adminId != null && !adminId.trim().isEmpty() && !"undefined".equalsIgnoreCase(adminId) && !"null".equalsIgnoreCase(adminId)) {
                admin = adminRepository.findById(adminId.trim());
                if (admin.isEmpty()) {
                    admin = adminRepository.findByUsername(adminId.trim());
                }
            }

            // Fallback for staff session if adminId is not in localStorage
            if (admin.isEmpty()) {
                admin = adminRepository.findByUsername("ghann");
                if (admin.isEmpty()) {
                    java.util.List<Admin> allAdmins = adminRepository.findAll();
                    if (!allAdmins.isEmpty()) {
                        admin = Optional.of(allAdmins.get(0));
                    }
                }
            }

            if (admin.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy tài khoản nhân viên hợp lệ!");
            }

            if (sessionAttributes != null) {
                sessionAttributes.put("role", "STAFF");
                sessionAttributes.put("adminId", admin.get().getId());
                sessionAttributes.put("sender", sender != null && !sender.isBlank() ? sender : admin.get().getUsername());
            }
        } else {
            // Default to CUSTOMER
            if (idBanStr == null || idBanStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Không xác định được bàn của khách hàng!");
            }

            try {
                int idBan = Integer.parseInt(idBanStr.trim());
                Optional<BanAn> banAn = banAnRepository.findByIdBan(idBan);
                if (banAn.isEmpty()) {
                    throw new IllegalArgumentException("Bàn " + idBan + " không tồn tại trong hệ thống!");
                }

                if (sessionAttributes != null) {
                    sessionAttributes.put("role", "CUSTOMER");
                    sessionAttributes.put("idBan", idBan);
                    sessionAttributes.put("sender", sender != null && !sender.isBlank() ? sender : "Bàn " + idBan);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Mã bàn không hợp lệ: " + idBanStr);
            }
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new SecurityException("Không tìm thấy phiên kết nối hợp lệ!");
        }

        String role = (String) sessionAttributes.get("role");

        if ("CUSTOMER".equals(role)) {
            Object idBanObj = sessionAttributes.get("idBan");
            if (idBanObj == null) {
                throw new SecurityException("Không có quyền đăng ký nhận tin!");
            }

            int idBan = (int) idBanObj;
            String allowedTopic = "/topic/chat/table/" + idBan;

            // Customers are ONLY permitted to subscribe to their own table topic
            if (!allowedTopic.equals(destination)) {
                throw new SecurityException("Khách hàng chỉ được phép nhận tin nhắn từ bàn của mình!");
            }
        } else if ("STAFF".equals(role)) {
            // Staff can subscribe to /topic/chat/table/* or /topic/chat/staff
            if (!destination.startsWith("/topic/chat/table/") && !destination.equals("/topic/chat/staff")) {
                throw new SecurityException("Kênh nhận tin không hợp lệ cho nhân viên!");
            }
        } else {
            throw new SecurityException("Vai trò không xác định!");
        }
    }
}
