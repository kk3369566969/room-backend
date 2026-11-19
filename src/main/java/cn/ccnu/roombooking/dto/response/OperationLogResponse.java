package cn.ccnu.roombooking.dto.response;

import cn.ccnu.roombooking.entity.OperationLog;
import cn.ccnu.roombooking.entity.User;
import cn.ccnu.roombooking.enums.ObjectType;
import java.time.LocalDateTime;
import java.util.UUID;

public record OperationLogResponse(
    UUID id,
    UserResponse operator,
    ObjectType objectType,
    UUID objectId,
    String action,
    String result,
    String summary,
    LocalDateTime createdAt) {

  public static OperationLogResponse of(OperationLog log, User operator) {
    return new OperationLogResponse(log.getId(), UserResponse.of(operator), log.getObjectType(),
        log.getObjectId(), log.getAction(), log.getResult(), log.getSummary(), log.getCreatedAt());
  }
}
