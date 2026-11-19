package cn.ccnu.roombooking.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理：把各类异常收敛为统一错误结构，避免异常堆栈泄漏给客户端。
 *
 * <p>日志分级：业务异常（可预期的 4xx）记 WARN 并带上错误码与路径，属于「正常被拒绝」的调用；
 * 未预期异常记 ERROR 并保留堆栈。两者都带 traceId，可与同一次请求的业务日志对上。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorBody> handleApi(ApiException e, HttpServletRequest request) {
    log.warn("[业务异常] {} {} -> {} {} : {}", request.getMethod(), request.getRequestURI(),
        e.getStatus().value(), e.getCode(), e.getMessage());
    return ResponseEntity.status(e.getStatus()).body(ErrorBody.of(e.getCode(), e.getMessage(), null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException e,
                                                    HttpServletRequest request) {
    var details = e.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .toList();
    log.warn("[参数校验失败] {} {} -> {}", request.getMethod(), request.getRequestURI(), details);
    return ResponseEntity.badRequest().body(ErrorBody.of("VALIDATION_ERROR", "请求参数不合法", details));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorBody> handleConstraint(ConstraintViolationException e,
                                                    HttpServletRequest request) {
    log.warn("[参数校验失败] {} {} -> {}", request.getMethod(), request.getRequestURI(), e.getMessage());
    return ResponseEntity.badRequest().body(ErrorBody.of("VALIDATION_ERROR", e.getMessage(), null));
  }

  @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
  public ResponseEntity<ErrorBody> handleBadParameter(Exception e, HttpServletRequest request) {
    log.warn("[参数不合法] {} {} -> {}", request.getMethod(), request.getRequestURI(), e.getMessage());
    return ResponseEntity.badRequest().body(ErrorBody.of("VALIDATION_ERROR", "请求参数不合法", e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorBody> handleOther(Exception e, HttpServletRequest request) {
    log.error("[未处理异常] {} {}", request.getMethod(), request.getRequestURI(), e);
    return ResponseEntity.internalServerError().body(ErrorBody.of("INTERNAL_ERROR", "服务器处理失败", null));
  }
}
