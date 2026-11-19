package cn.ccnu.roombooking.service.impl;

import cn.ccnu.roombooking.entity.Notification;
import cn.ccnu.roombooking.entity.OperationLog;
import cn.ccnu.roombooking.enums.NotificationType;
import cn.ccnu.roombooking.enums.ObjectType;
import cn.ccnu.roombooking.mapper.NotificationMapper;
import cn.ccnu.roombooking.mapper.OperationLogMapper;
import cn.ccnu.roombooking.service.AuditService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** {@link AuditService} 的默认实现：通知与操作日志的简单落库。 */
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

  private final NotificationMapper notifications;
  private final OperationLogMapper operationLogs;

  @Override
  public void notify(UUID recipientId, NotificationType type, UUID businessId, String content) {
    Notification notification = new Notification();
    notification.setRecipientId(recipientId);
    notification.setType(type);
    notification.setBusinessId(businessId);
    notification.setContent(content);
    notifications.insert(notification);
  }

  @Override
  public void log(UUID operatorId, ObjectType objectType, UUID objectId, String action,
                  String result, String summary) {
    OperationLog log = new OperationLog();
    log.setOperatorId(operatorId);
    log.setObjectType(objectType);
    log.setObjectId(objectId);
    log.setAction(action);
    log.setResult(result);
    log.setSummary(summary);
    operationLogs.insert(log);
  }
}
