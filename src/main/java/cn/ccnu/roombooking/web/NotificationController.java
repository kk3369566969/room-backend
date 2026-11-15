package cn.ccnu.roombooking.web;
import cn.ccnu.roombooking.common.*;
import cn.ccnu.roombooking.domain.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
  private final AuthContext auth; private final NotificationRepository notifications;
  public NotificationController(AuthContext auth, NotificationRepository notifications) { this.auth=auth; this.notifications=notifications; }
  @GetMapping List<ApiDtos.NotificationResponse> list() { User u=auth.approved(); return notifications.findByRecipientOrderByCreatedAtDesc(u).stream().map(this::view).toList(); }
  @PostMapping("/{id}/read") ApiDtos.NotificationResponse read(@PathVariable UUID id) { User u=auth.approved(); var n=notifications.findById(id).orElseThrow(()->new ApiException(org.springframework.http.HttpStatus.NOT_FOUND,"NOTIFICATION_NOT_FOUND","通知不存在")); if(!n.getRecipient().getId().equals(u.getId())) throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,"FORBIDDEN","无权操作此通知"); n.setReadAt(LocalDateTime.now()); return view(notifications.save(n)); }
  private ApiDtos.NotificationResponse view(Notification n) { return new ApiDtos.NotificationResponse(n.getId().toString(),n.getType(),n.getBusinessId(),n.getContent(),n.getCreatedAt(),n.getReadAt()); }
}
