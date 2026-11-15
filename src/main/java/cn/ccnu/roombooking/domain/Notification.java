package cn.ccnu.roombooking.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="notification") @Getter @Setter @NoArgsConstructor
public class Notification {
  @Id private UUID id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="recipient_id", nullable=false) private User recipient;
  @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) private Enums.NotificationType type;
  @Column(name="business_id") private UUID businessId;
  @Column(nullable=false) private String content;
  @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
  private LocalDateTime readAt;
}
