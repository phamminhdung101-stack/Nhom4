package chandanv.local.chandanv.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/** Trả lỗi dạng JSON thống nhất {status, message, time} thay vì stacktrace / 500 chung chung. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Khách tự ngắt kết nối giữa chừng (đóng tab / chuyển trang khi ảnh chưa tải xong, mất mạng):
     * không phải lỗi máy chủ -> không ghi ERROR kèm stacktrace làm rối log, cũng không cần trả gì (kết nối đã đóng).
     */
    @ExceptionHandler({ClientAbortException.class, AsyncRequestNotUsableException.class})
    public void handleClientAbort(Exception ex) {
        log.debug("Client ngắt kết nối: {}", ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        return body(ex.getStatusCode(), ex.getReason() != null ? ex.getReason() : "Yêu cầu không hợp lệ");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex) {
        return body(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateKeyException ex) {
        return body(HttpStatus.CONFLICT, "Dữ liệu bị trùng, vui lòng thử lại");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        if (ex instanceof ErrorResponse er) { // lỗi chuẩn của Spring MVC: 404 tài nguyên, 405, thiếu tham số...
            return body(er.getStatusCode(), er.getBody().getDetail() != null ? er.getBody().getDetail() : ex.getMessage());
        }
        log.error("Lỗi không mong muốn", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi máy chủ, vui lòng thử lại");
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatusCode status, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status.value());
        m.put("message", message);
        m.put("time", Instant.now().toString());
        return ResponseEntity.status(status).body(m);
    }
}
