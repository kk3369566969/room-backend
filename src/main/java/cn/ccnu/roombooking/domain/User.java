package cn.ccnu.roombooking.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="app_user") @Getter @Setter @NoArgsConstructor
public class User {
  @Id private UUID id;
  @Column(name="external_id", nullable=false, unique=true, length=64) private String externalId;
  @Column(nullable=false, length=50) private String name;
  @Column(name="employee_no", nullable=false, unique=true, length=30) private String employeeNo;
  @Enumerated(EnumType.STRING) @Column(name="member_type", nullable=false, length=16) private Enums.MemberType memberType;
  @Column(nullable=false, length=20) private String phone;
  @Enumerated(EnumType.STRING) @Column(name="identity_status", nullable=false, length=16) private Enums.IdentityStatus identityStatus;
  @Column(nullable=false) private boolean manager;
  @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
  @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
}
