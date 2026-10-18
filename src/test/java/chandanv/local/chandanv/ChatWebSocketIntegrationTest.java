package chandanv.local.chandanv;

import chandanv.local.chandanv.controllers.ChatController;
import chandanv.local.chandanv.models.dto.ChatMessage;
import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.repositories.AdminRepository;
import chandanv.local.chandanv.repositories.BanAnRepository;
import chandanv.local.chandanv.websocket.ChatSecurityChannelInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ChatWebSocketIntegrationTest {

    @Autowired
    private ChatSecurityChannelInterceptor interceptor;

    @Autowired
    private ChatController chatController;

    @MockBean
    private BanAnRepository banAnRepository;

    @MockBean
    private AdminRepository adminRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    public void setup() {
        BanAn ban1 = new BanAn();
        ban1.setIdBan(1);
        ban1.setTenBan("Bàn 01");
        when(banAnRepository.findByIdBan(1)).thenReturn(Optional.of(ban1));
        when(banAnRepository.findByIdBan(99)).thenReturn(Optional.empty());

        Admin admin = new Admin();
        admin.setId("admin123");
        admin.setUsername("testadmin");
        when(adminRepository.findById("admin123")).thenReturn(Optional.of(admin));
        when(adminRepository.findByUsername("testadmin")).thenReturn(Optional.of(admin));
    }

    @Test
    public void testCustomerConnectValidTable() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("role", "CUSTOMER");
        accessor.addNativeHeader("idBan", "1");
        accessor.setSessionAttributes(new HashMap<>());

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        assertNotNull(result);

        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        assertEquals("CUSTOMER", sessionAttrs.get("role"));
        assertEquals(1, sessionAttrs.get("idBan"));
    }

    @Test
    public void testCustomerConnectInvalidTableThrowsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("role", "CUSTOMER");
        accessor.addNativeHeader("idBan", "99");
        accessor.setSessionAttributes(new HashMap<>());

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        assertThrows(IllegalArgumentException.class, () -> {
            interceptor.preSend(message, channel);
        });
    }

    @Test
    public void testCustomerSubscribeOwnTableAllowed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/table/1");
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("role", "CUSTOMER");
        sessionAttrs.put("idBan", 1);
        accessor.setSessionAttributes(sessionAttrs);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        assertNotNull(result);
    }

    @Test
    public void testCustomerSubscribeOtherTableForbidden() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/table/2"); // Customer is at table 1, attempts table 2
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("role", "CUSTOMER");
        sessionAttrs.put("idBan", 1);
        accessor.setSessionAttributes(sessionAttrs);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        assertThrows(SecurityException.class, () -> {
            interceptor.preSend(message, channel);
        });
    }

    @Test
    public void testStaffConnectValidAdmin() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("role", "STAFF");
        accessor.addNativeHeader("adminId", "admin123");
        accessor.setSessionAttributes(new HashMap<>());

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        assertNotNull(result);

        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        assertEquals("STAFF", sessionAttrs.get("role"));
    }

    @Test
    public void testStaffSubscribeAllTablesAllowed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/table/5");
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("role", "STAFF");
        accessor.setSessionAttributes(sessionAttrs);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        assertNotNull(result);
    }

    @Test
    public void testCustomerSendMessageSanitizesHtmlAndForcesTableId() {
        ChatMessage incoming = new ChatMessage();
        incoming.setIdBan(99); // Attacker tries to pretend to be table 99
        incoming.setContent("Cho em xin <script>alert('hack')</script> thêm đá");

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("role", "CUSTOMER");
        sessionAttrs.put("idBan", 1); // Authenticated session is table 1
        sessionAttrs.put("sender", "Bàn 1");
        headerAccessor.setSessionAttributes(sessionAttrs);

        chatController.sendMessage(incoming, headerAccessor);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/table/1"), captor.capture());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/staff"), captor.capture());

        ChatMessage sent = captor.getValue();
        // Verifies table ID was forced to authenticated session table ID
        assertEquals(1, sent.getIdBan());
        // Verifies XSS was sanitized
        assertFalse(sent.getContent().contains("<script>"));
        assertTrue(sent.getContent().contains("&lt;script&gt;"));
    }

    @Test
    public void testStaffSendMessageToCustomerTable() {
        ChatMessage incoming = new ChatMessage();
        incoming.setIdBan(1);
        incoming.setContent("Dạ em mang đá ra ngay ạ!");

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("role", "STAFF");
        sessionAttrs.put("sender", "testadmin");
        headerAccessor.setSessionAttributes(sessionAttrs);

        chatController.sendMessage(incoming, headerAccessor);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/table/1"), captor.capture());

        ChatMessage sent = captor.getValue();
        assertEquals(1, sent.getIdBan());
        assertEquals("STAFF", sent.getRole());
        assertEquals("Dạ em mang đá ra ngay ạ!", sent.getContent());
    }
}

