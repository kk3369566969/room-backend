package cn.ccnu.roombooking.service.impl;

import cn.ccnu.roombooking.common.ApiException;
import cn.ccnu.roombooking.dto.response.NotificationResponse;
import cn.ccnu.roombooking.entity.Notification;
import cn.ccnu.roombooking.entity.User;
import cn.ccnu.roombooking.mapper.NotificationMapper;
import cn.ccnu.roombooking.security.AuthContext;
import cn.ccnu.roombooking.service.NotificationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link NotificationService} 的默认实现：按接收人隔离的通知读取与已读标记。 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationMapper notifications;
  private final AuthContext authContext;

  @Override
  public List<NotificationResponse> listMine() {
    User user = authContext.approved();
    return notifications.selectList(Wrappers.<Notification>lambdaQuery()
            .eq(Notification::getRecipientId, user.getId())
            .orderByDesc(Notification::getCreatedAt))
        .stream()
        .map(NotificationResponse::of)
        .toList();
  }

  @Override
  @Transactional
  public NotificationResponse markRead(UUID id) {
    User user = authContext.approved();
    Notification notification = notifications.selectById(id);
    if (notification == null) {
      throw ApiException.notFound("NOTIFICATION_NOT_FOUND", "通知不存在");
    }
    // 越权标记他人通知会造成已读状态串扰，直接拒绝
    if (!notification.getRecipientId().equals(user.getId())) {
      throw ApiException.forbidden("FORBIDDEN", "无权操作此通知");
    }
    notification.setReadAt(LocalDateTime.now());
    notifications.updateById(notification);
    return NotificationResponse.of(notification);
  }
}
