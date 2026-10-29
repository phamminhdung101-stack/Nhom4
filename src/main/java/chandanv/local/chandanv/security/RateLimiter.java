package chandanv.local.chandanv.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Bộ giới hạn tần suất đơn giản (cửa sổ cố định) lưu trong RAM.
 * Dùng chung cho: request API theo IP, đăng nhập, tin nhắn WebSocket/chat.
 */
@Component
public class RateLimiter {

    private static final class Window {
        long start;
        int count;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @return true nếu còn được phép (và tính thêm 1 lần), false nếu đã vượt limit trong cửa sổ windowMs.
     */
    public boolean tryAcquire(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        Window w = windows.computeIfAbsent(key, k -> new Window());
        synchronized (w) {
            if (now - w.start >= windowMs) {
                w.start = now;
                w.count = 0;
            }
            if (w.count >= limit) {
                return false;
            }
            w.count++;
            return true;
        }
    }

    /** Số giây còn lại tới khi cửa sổ của key được làm mới. */
    public long secondsUntilReset(String key, long windowMs) {
        Window w = windows.get(key);
        if (w == null) return 0;
        synchronized (w) {
            long left = windowMs - (System.currentTimeMillis() - w.start);
            return Math.max(1, (left + 999) / 1000);
        }
    }

    public void reset(String key) {
        windows.remove(key);
    }

    /** Dọn các cửa sổ cũ để map không phình to (giữ tối đa 1 giờ). */
    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now - e.getValue().start > 3_600_000);
    }

    int size() {
        return windows.size();
    }
}
