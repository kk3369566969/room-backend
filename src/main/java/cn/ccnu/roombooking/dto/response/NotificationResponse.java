package cn.ccnu.roombooking.dto.response;

import cn.ccnu.roombooking.entity.Notification;
import cn.ccnu.roombooking.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
    String id,
    NotificationType type,
    UUID businessId,
    String content,
    LocalDateTime createdAt,
    LocalDateTime readAt) {

  public static NotificationResponse of(Notification notification) {
    return new NotificationResponse(notification.getId().toString(), notification.getType(),
        notification.getBusinessId(), notification.getContent(), notification.getCreatedAt(),
        notification.getReadAt());
  }
}
