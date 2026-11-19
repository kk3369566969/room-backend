package cn.ccnu.roombooking.common;

import java.time.OffsetDateTime;

/**
 * 统一错误响应体，字段与 OpenAPI 契约 ErrorBody 保持一致。
 */
public record ErrorBody(String code, String message, Object details, OffsetDateTime timestamp) {

  public static ErrorBody of(String code, String message, Object details) {
    return new ErrorBody(code, message, details, OffsetDateTime.now());
  }
}
