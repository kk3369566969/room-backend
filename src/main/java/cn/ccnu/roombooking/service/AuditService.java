package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.domain.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditService {
  private final NotificationRepository notifications; private final OperationLogRepository logs;
  public AuditService(NotificationRepository notifications, OperationLogRepository logs) { this.notifications=notifications; this.logs=logs; }
  public void notify(User user, Enums.NotificationType type, UUID businessId, String content) { var n=new Notification(); n.setId(UUID.randomUUID()); n.setRecipient(user); n.setType(type); n.setBusinessId(businessId); n.setContent(content); n.setCreatedAt(LocalDateTime.now()); notifications.save(n); }
  public void log(User operator, Enums.ObjectType type, UUID objectId, String action, String result, String summary) { var l=new OperationLog(); l.setId(UUID.randomUUID()); l.setOperator(operator); l.setObjectType(type); l.setObjectId(objectId); l.setAction(action); l.setResult(result); l.setSummary(summary); l.setCreatedAt(LocalDateTime.now()); logs.save(l); }
}
