package cn.ccnu.roombooking.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
  Optional<AuthSession> findByTokenHash(String tokenHash);
}
