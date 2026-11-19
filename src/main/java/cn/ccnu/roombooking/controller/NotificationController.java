package cn.ccnu.roombooking.controller;

import cn.ccnu.roombooking.dto.response.NotificationResponse;
import cn.ccnu.roombooking.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public List<NotificationResponse> list() {
    return notificationService.listMine();
  }

  @PostMapping("/{id}/read")
  public NotificationResponse read(@PathVariable UUID id) {
    return notificationService.markRead(id);
  }
}
