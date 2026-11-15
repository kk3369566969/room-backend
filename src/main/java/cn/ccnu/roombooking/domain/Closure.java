package cn.ccnu.roombooking.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="closure") @Getter @Setter @NoArgsConstructor
public class Closure {
  @Id private UUID id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="room_id", nullable=false) private Room room;
  @Column(name="start_at", nullable=false) private LocalDateTime startAt;
  @Column(name="end_at", nullable=false) private LocalDateTime endAt;
  @Column(nullable=false) private String reason;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="manager_id", nullable=false) private User manager;
  @Column(nullable=false) private boolean active;
  @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
}
