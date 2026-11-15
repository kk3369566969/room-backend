package cn.ccnu.roombooking.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_session", indexes = @Index(name = "idx_auth_session_external", columnList = "external_id"))
@Getter @Setter @NoArgsConstructor
public class AuthSession {
  @Id private UUID id;
  @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
  @Column(name = "external_id", nullable = false, length = 64) private String externalId;
  @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
  @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
  @Column(name = "revoked_at") private LocalDateTime revokedAt;
}
