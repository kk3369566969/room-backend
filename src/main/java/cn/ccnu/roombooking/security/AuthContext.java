package cn.ccnu.roombooking.security;

import cn.ccnu.roombooking.common.ApiException;
import cn.ccnu.roombooking.entity.AuthSession;
import cn.ccnu.roombooking.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前请求的登录身份。
 *
 * <p>由 {@link AuthFilter} 写入 Servlet 请求属性，业务层从这里读取。
 * 之所以不直接用 {@code @RequestScope} Bean，是因为过滤器早于 DispatcherServlet 执行，
 * 那时请求作用域尚未激活；用请求属性传递可以避开这个坑。
 */
@Component
public class AuthContext {

  /** 开发模式是否生效，由 {@link AuthFilter} 写入，用于把 401 提示说得更具体 */
  static final String ATTR_DEV_MODE = AuthContext.class.getName() + ".devMode";
  private static final String ATTR_USER = AuthContext.class.getName() + ".user";
  private static final String ATTR_SESSION = AuthContext.class.getName() + ".session";
  private static final String ATTR_EXTERNAL_ID = AuthContext.class.getName() + ".externalId";

  /** 登录态：微信会话 + 对应用户（用户可能尚未提交身份资料，故允许为空） */
  static void bind(HttpServletRequest request, AuthSession session, User user) {
    request.setAttribute(ATTR_SESSION, session);
    request.setAttribute(ATTR_EXTERNAL_ID, session.getExternalId());
    if (user != null) {
      request.setAttribute(ATTR_USER, user);
    }
  }

  /** 本地联调：仅凭 X-User-Id 识别身份 */
  static void bindExternalId(HttpServletRequest request, String externalId, User user) {
    request.setAttribute(ATTR_EXTERNAL_ID, externalId);
    if (user != null) {
      request.setAttribute(ATTR_USER, user);
    }
  }

  public AuthSession session() {
    AuthSession session = (AuthSession) currentRequest().getAttribute(ATTR_SESSION);
    if (session == null) {
      throw ApiException.unauthorized("UNAUTHORIZED", "需要有效的微信登录会话");
    }
    return session;
  }

  public String externalId() {
    return (String) currentRequest().getAttribute(ATTR_EXTERNAL_ID);
  }

  public User user() {
    User user = (User) currentRequest().getAttribute(ATTR_USER);
    if (user == null) {
      throw ApiException.unauthorized("UNAUTHORIZED", unauthenticatedMessage());
    }
    return user;
  }

  /** 要求身份已核验通过 */
  public User approved() {
    User current = user();
    if (!current.isApproved()) {
      throw ApiException.forbidden("IDENTITY_REQUIRED", "身份尚未核验通过");
    }
    return current;
  }

  /** 要求负责人权限 */
  public User manager() {
    User current = approved();
    if (!current.isManager()) {
      throw ApiException.forbidden("MANAGER_REQUIRED", "需要负责人权限");
    }
    return current;
  }

  /**
   * 组装未登录时的提示。
   *
   * <p>开发模式下把「为什么没识别到」和「该怎么改」直接写进 message，
   * 免得在 Apifox 里只能看到一个干巴巴的 401 去翻代码。
   */
  private String unauthenticatedMessage() {
    HttpServletRequest request = currentRequest();
    if (!Boolean.TRUE.equals(request.getAttribute(ATTR_DEV_MODE))) {
      return "缺少有效登录身份";
    }
    String externalId = externalId();
    if (StringUtils.hasText(externalId)) {
      return "账号 [" + externalId + "] 尚未建立用户档案，请先调用 POST /api/v1/identity/reviews 提交身份资料，"
          + "或把请求头 X-User-Id 换成 student-demo / teacher-demo / manager-demo";
    }
    return "未识别到登录身份：可携带 X-User-Id（student-demo / teacher-demo / manager-demo），"
        + "或删除 Authorization 请求头后完全不带凭证访问（将使用默认负责人账号）";
  }

  private HttpServletRequest currentRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletAttributes) {
      return servletAttributes.getRequest();
    }
    throw ApiException.unauthorized("UNAUTHORIZED", "当前上下文不存在 HTTP 请求");
  }
}
