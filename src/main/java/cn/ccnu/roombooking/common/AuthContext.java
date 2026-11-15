package cn.ccnu.roombooking.common;

import cn.ccnu.roombooking.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Component @RequestScope
public class AuthContext {
  private User user; private String externalId; private AuthSession session;
  public void setExternalId(String externalId) { this.externalId = externalId; }
  public String externalId() { return externalId; }
  public void setUser(User user) { this.user = user; }
  public void setSession(AuthSession session) { this.session = session; }
  public AuthSession session() { if (session == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "需要有效的微信登录会话"); return session; }
  public User user() { if (user == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "缺少有效登录身份"); return user; }
  public User approved() { User u=user(); if (u.getIdentityStatus()!=Enums.IdentityStatus.APPROVED) throw new ApiException(HttpStatus.FORBIDDEN,"IDENTITY_REQUIRED","身份尚未核验通过"); return u; }
  public User manager() { User u=approved(); if (!u.isManager()) throw new ApiException(HttpStatus.FORBIDDEN,"MANAGER_REQUIRED","需要负责人权限"); return u; }
}
