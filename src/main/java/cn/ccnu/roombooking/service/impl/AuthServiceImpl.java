package cn.ccnu.roombooking.service.impl;

import cn.ccnu.roombooking.client.WechatClient;
import cn.ccnu.roombooking.common.log.Logs;
import cn.ccnu.roombooking.config.properties.AuthProperties;
import cn.ccnu.roombooking.dto.response.LoginResponse;
import cn.ccnu.roombooking.entity.AuthSession;
import cn.ccnu.roombooking.entity.User;
import cn.ccnu.roombooking.mapper.AuthSessionMapper;
import cn.ccnu.roombooking.mapper.UserMapper;
import cn.ccnu.roombooking.security.AuthContext;
import cn.ccnu.roombooking.security.TokenHasher;
import cn.ccnu.roombooking.service.AuthService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link AuthService} 的默认实现：微信 code 换 openid，签发并吊销本地会话令牌。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final WechatClient wechatClient;
  private final AuthSessionMapper sessions;
  private final UserMapper users;
  private final AuthContext authContext;
  private final AuthProperties authProperties;

  @Override
  @Transactional
  public LoginResponse login(String code) {
    String externalId = wechatClient.exchangeCode(code);
    long ttlSeconds = authProperties.sessionTtlSeconds();
    String token = TokenHasher.randomToken();
    LocalDateTime now = LocalDateTime.now();

    // 只落库摘要，即使数据库泄露也无法还原出可用令牌
    AuthSession session = new AuthSession();
    session.setTokenHash(TokenHasher.sha256(token));
    session.setExternalId(externalId);
    session.setExpiresAt(now.plusSeconds(ttlSeconds));
    sessions.insert(session);

    // 用户档案可能尚未建立（首次登录还没提交身份资料），此时返回 registered=false 让前端引导
    User user = users.findByExternalId(externalId).orElse(null);
    boolean registered = user != null;
    String identityStatus = user == null || user.getIdentityStatus() == null
        ? null : user.getIdentityStatus().name();
    String memberType = user == null || user.getMemberType() == null
        ? null : user.getMemberType().name();

    log.info("[登录] 会话签发成功 externalId={} 有效期={}s 已建档={} 身份状态={} 身份类型={}",
        Logs.abbrev(externalId), ttlSeconds, registered,
        identityStatus == null ? "无档案" : identityStatus,
        memberType == null ? "-" : memberType);

    return new LoginResponse(token, "Bearer", ttlSeconds, externalId, registered,
        identityStatus, memberType);
  }

  @Override
  @Transactional
  public void logout() {
    AuthSession session = authContext.session();
    session.setRevokedAt(LocalDateTime.now());
    sessions.updateById(session);
    log.info("[登出] 会话已吊销 externalId={}", Logs.abbrev(session.getExternalId()));
  }
}
