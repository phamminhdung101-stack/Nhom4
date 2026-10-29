package chandanv.local.chandanv.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Chống dò mật khẩu (brute force). Ngưỡng chỉnh trong application.properties, mặc định nới cho demo:
 *  - Sai quá N lần (mặc định 10) với cùng tài khoản + IP -> khoá 1 phút.
 *  - Một IP sai quá 30 lần (thử nhiều tài khoản khác nhau) -> khoá IP 5 phút.
 */
@Service
public class LoginAttemptService {

    private static final class Attempt {
        int failures;
        long firstFailAt;
        long lockedUntil;
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final long lockMs;
    private final int maxFailuresPerIp;
    private final long ipLockMs;
    private static final long FAILURE_WINDOW_MS = 15 * 60_000;

    public LoginAttemptService(@Value("${app.security.login-max-failures:10}") int maxFailures,
                               @Value("${app.security.login-lock-minutes:1}") long lockMinutes,
                               @Value("${app.security.login-ip-max-failures:30}") int maxFailuresPerIp,
                               @Value("${app.security.login-ip-lock-minutes:5}") long ipLockMinutes) {
        this.maxFailures = maxFailures;
        this.lockMs = lockMinutes * 60_000;
        this.maxFailuresPerIp = maxFailuresPerIp;
        this.ipLockMs = ipLockMinutes * 60_000;
    }

    static String userKey(String username, String ip) {
        return "u:" + (username == null ? "" : username.trim().toLowerCase()) + "|" + ip;
    }

    static String ipKey(String ip) {
        return "ip:" + ip;
    }

    /** Ném 429 nếu tài khoản/IP đang bị khoá. */
    public void kiemTraKhoa(String username, String ip) {
        long now = System.currentTimeMillis();
        for (String key : new String[] {userKey(username, ip), ipKey(ip)}) {
            Attempt a = attempts.get(key);
            if (a != null) {
                synchronized (a) {
                    if (a.lockedUntil > now) {
                        long phut = Math.max(1, (a.lockedUntil - now + 59_999) / 60_000);
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                "Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + phut + " phút.");
                    }
                }
            }
        }
    }

    /** Ghi nhận 1 lần sai. Trả về số lần thử còn lại trước khi bị khoá (0 = vừa bị khoá). */
    public int dangNhapSai(String username, String ip) {
        tang(ipKey(ip), maxFailuresPerIp, ipLockMs);
        return tang(userKey(username, ip), maxFailures, lockMs);
    }

    public void dangNhapDung(String username, String ip) {
        attempts.remove(userKey(username, ip));
    }

    private int tang(String key, int max, long lock) {
        long now = System.currentTimeMillis();
        Attempt a = attempts.computeIfAbsent(key, k -> new Attempt());
        synchronized (a) {
            if (a.failures == 0 || now - a.firstFailAt > FAILURE_WINDOW_MS || (a.lockedUntil != 0 && a.lockedUntil <= now)) {
                a.failures = 0;
                a.firstFailAt = now;
                a.lockedUntil = 0;
            }
            a.failures++;
            if (a.failures >= max) {
                a.lockedUntil = now + lock;
                return 0;
            }
            return max - a.failures;
        }
    }

    @Scheduled(fixedRate = 600_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> e.getValue().lockedUntil < now && now - e.getValue().firstFailAt > FAILURE_WINDOW_MS);
    }
}
