package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.common.*;
import cn.ccnu.roombooking.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {
  private final WechatClient wechat;
  private final AuthSessionRepository sessions;
  private final UserRepository users;
  private final AuthContext auth;
  private final long ttlSeconds;

  public AuthService(WechatClient wechat, AuthSessionRepository sessions, UserRepository users, AuthContext auth,
                     @Value("${app.auth.session-ttl-seconds:604800}") long ttlSeconds) {
    this.wechat = wechat;
    this.sessions = sessions;
    this.users = users;
    this.auth = auth;
    this.ttlSeconds = ttlSeconds;
  }

  @Transactional
  public ApiDtos.LoginResponse login(String code) {
    String externalId = wechat.exchangeCode(code);
    String token = randomToken();
    LocalDateTime now = LocalDateTime.now();
    var session = new AuthSession();
    session.setId(UUID.randomUUID());
    session.setTokenHash(hash(token));
    session.setExternalId(externalId);
    session.setCreatedAt(now);
    session.setExpiresAt(now.plusSeconds(ttlSeconds));
    sessions.save(session);
    var user = users.findByExternalId(externalId).orElse(null);
    return new ApiDtos.LoginResponse(token, "Bearer", ttlSeconds, externalId, user != null,
        user == null ? null : user.getIdentityStatus(), user == null ? null : user.getMemberType());
  }

  @Transactional
  public void logout() {
    var session = auth.session();
    session.setRevokedAt(LocalDateTime.now());
    sessions.save(session);
  }

  public static String hash(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private String randomToken() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
