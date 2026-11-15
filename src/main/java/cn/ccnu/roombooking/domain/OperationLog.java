package cn.ccnu.roombooking.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="operation_log") @Getter @Setter @NoArgsConstructor
public class OperationLog {
  @Id private UUID id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="operator_id") private User operator;
  @Enumerated(EnumType.STRING) @Column(name="object_type", nullable=false, length=32) private Enums.ObjectType objectType;
  @Column(name="object_id") private UUID objectId;
  @Column(nullable=false, length=32) private String action;
  @Column(nullable=false, length=16) private String result;
  private String summary;
  @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
}
