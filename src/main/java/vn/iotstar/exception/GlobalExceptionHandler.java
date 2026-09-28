package vn.iotstar.exception;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccountStatusException.class)
    ProblemDetail account(AccountStatusException ex) { return problem(403, "Tài khoản bị khóa hoặc vô hiệu hóa"); }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> authentication(AuthenticationException ex) {
        return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer")
                .body(problem(401, "Email hoặc mật khẩu không đúng"));
    }
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException ex) { return problem(403, "Bạn không có quyền truy cập"); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail duplicate(DataIntegrityViolationException ex) { return problem(409, "Dữ liệu bị trùng hoặc không hợp lệ"); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ProblemDetail badRequest(Exception ex) { return problem(400, "Kiểm tra email, họ tên và mật khẩu (6-72 ký tự khi đăng ký)"); }
    private ProblemDetail problem(int status, String detail) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
    }
}
