package cn.ccnu.roombooking.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  public record ErrorBody(String code, String message, Object details, OffsetDateTime timestamp) {}
  @ExceptionHandler(ApiException.class) ResponseEntity<ErrorBody> handle(ApiException e) { return ResponseEntity.status(e.getStatus()).body(new ErrorBody(e.getCode(),e.getMessage(),null,OffsetDateTime.now())); }
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException e) { return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION_ERROR","请求参数不合法",e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).toList(),OffsetDateTime.now())); }
  @ExceptionHandler(ConstraintViolationException.class) ResponseEntity<ErrorBody> handleConstraint(ConstraintViolationException e) { return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION_ERROR",e.getMessage(),null,OffsetDateTime.now())); }
  @ExceptionHandler(Exception.class) ResponseEntity<ErrorBody> handleOther(Exception e) { return ResponseEntity.status(500).body(new ErrorBody("INTERNAL_ERROR","服务器处理失败",null,OffsetDateTime.now())); }
}
