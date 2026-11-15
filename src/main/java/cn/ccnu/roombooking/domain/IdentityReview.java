package cn.ccnu.roombooking.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="identity_review") @Getter @Setter @NoArgsConstructor
public class IdentityReview {
  @Id private UUID id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false) private User user;
  @Column(name="name_snapshot", nullable=false) private String nameSnapshot;
  @Column(name="employee_no_snapshot", nullable=false) private String employeeNoSnapshot;
  @Enumerated(EnumType.STRING) @Column(name="member_type_snapshot", nullable=false) private Enums.MemberType memberTypeSnapshot;
  @Column(name="phone_snapshot", nullable=false) private String phoneSnapshot;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Enums.ReviewStatus status;
  private String reason;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewer_id") private User reviewer;
  @Column(name="submitted_at", nullable=false) private LocalDateTime submittedAt;
  private LocalDateTime handledAt;
}
