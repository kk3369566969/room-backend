package cn.ccnu.roombooking.security;

import cn.ccnu.roombooking.common.log.Logs;
import cn.ccnu.roombooking.common.log.TraceIdFilter;
import cn.ccnu.roombooking.config.properties.AuthProperties;
import cn.ccnu.roombooking.entity.AuthSession;
import cn.ccnu.roombooking.entity.User;
import cn.ccnu.roombooking.mapper.AuthSessionMapper;
import cn.ccnu.roombooking.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 登录态解析过滤器。
 *
 * <p>身份来源按优先级依次尝试：
 * <ol>
 *   <li>{@code Authorization: Bearer <token>}：微信登录后签发，线上使用；</li>
 *   <li>{@code X-User-Id: <externalId>}：开发模式下手动指定身份，仅 devMode 开启时生效；</li>
 *   <li>开发模式兜底：以上都没有（或凭证无效）时，自动以 {@code app.auth.dev-default-user}
 *       账号登录，便于本地/Apifox 不带任何请求头直接调接口。</li>
 * </ol>
 *
 * <p>开发模式下「凭证无效」不会直接变成 401，而是继续回落到第 2、3 步。原因是在
 * Apifox/Postman 里很容易残留过期的 Bearer 令牌，或是把未定义的环境变量原样发成
 * {@code {{userId}}} 这样的占位符，如果严格按令牌优先处理，本地调试会一直 401 且看不出原因。
 *
 * <p>生产安全：devMode 关闭时 {@code X-User-Id} 与兜底身份都不生效；若激活了 {@code prod} profile，
 * 即使配置里写了 {@code dev-mode: true} 也会被强制关闭（启动时打 ERROR 日志），避免上线后仍能伪造身份。
 *
 * <p>解析结果写入请求属性，鉴权失败不在过滤器里拦截，由业务层按需抛出对应错误码。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String DEV_USER_HEADER = "X-User-Id";
  /** 视为生产环境的 profile 名称 */
  private static final List<String> PROD_PROFILES = List.of("prod", "production");
  /** 开发模式下可用的演示账号，仅用于日志与错误提示 */
  private static final String DEMO_ACCOUNTS = "student-demo / teacher-demo / manager-demo";

  private final AuthSessionMapper sessions;
  private final UserMapper users;
  private final AuthProperties authProperties;
  private final Environment environment;

  /** 实际生效的开发模式开关，启动时结合 profile 计算一次 */
  private boolean devModeEnabled;

  @PostConstruct
  void initDevMode() {
    boolean prodProfile = Arrays.stream(environment.getActiveProfiles())
        .anyMatch(PROD_PROFILES::contains);
    if (prodProfile && authProperties.devMode()) {
      log.error("[鉴权] 检测到生产 profile 但配置了 app.auth.dev-mode=true，已强制关闭开发身份，"
          + "请把 AUTH_DEV_MODE 设为 false");
      devModeEnabled = false;
      return;
    }
    devModeEnabled = authProperties.devMode();
    if (devModeEnabled) {
      log.warn("[鉴权] 开发模式已开启：不带任何凭证（或凭证无效）的请求将自动使用账号 [{}]，"
              + "X-User-Id 也可自由指定身份。生产环境请设置 AUTH_DEV_MODE=false 或激活 prod profile",
          authProperties.devDefaultUser());
    }
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
    // 把最终生效的开关交给业务层，401 时才能给出「该怎么带请求头」的提示
    request.setAttribute(AuthContext.ATTR_DEV_MODE, devModeEnabled);

    boolean bound = resolveByBearer(request);
    // 开发模式下令牌无效或未携带时继续回落，避免残留的过期令牌把本地调试挡成 401
    if (!bound && devModeEnabled) {
      bound = resolveByDevIdentity(request);
    }
    // 开发模式关闭且没有有效令牌时不绑定身份，后续由业务层抛出 401
    if (!bound) {
      log.debug("[鉴权] 未解析到登录身份，由业务层按需拒绝");
    }
    chain.doFilter(request, response);
  }

  /**
   * 尝试用 Bearer 令牌解析会话。
   *
   * @return 是否成功绑定了登录态
   */
  private boolean resolveByBearer(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      return false;
    }
    String token = authorization.substring(BEARER_PREFIX.length()).trim();
    if (token.isBlank()) {
      log.debug("[鉴权] Authorization 头缺少令牌内容");
      return false;
    }
    Optional<AuthSession> found = sessions.findByTokenHash(TokenHasher.sha256(token))
        .filter(AuthSession::isUsable);
    if (found.isEmpty()) {
      log.debug("[鉴权] Bearer 令牌无效或已过期");
      return false;
    }
    AuthSession session = found.get();
    User user = users.findByExternalId(session.getExternalId()).orElse(null);
    AuthContext.bind(request, session, user);
    bindUserLabel(user, session.getExternalId());
    return true;
  }

  /**
   * 开发模式下的身份解析：先看 {@code X-User-Id}，再回落到默认账号。
   *
   * @return 是否成功绑定了登录态
   */
  private boolean resolveByDevIdentity(HttpServletRequest request) {
    String devUserId = request.getHeader(DEV_USER_HEADER);
    if (StringUtils.hasText(devUserId) && !looksLikeTemplatePlaceholder(devUserId.trim())) {
      return resolveByDevHeader(request, devUserId.trim());
    }
    if (StringUtils.hasText(devUserId)) {
      log.warn("[鉴权] X-User-Id=[{}] 是未解析的模板占位符，已忽略并改用默认账号；"
              + "请在 Apifox 环境变量里为它赋值，或直接删除该请求头",
          devUserId.trim());
    }
    return resolveByDevFallback(request);
  }

  private boolean resolveByDevHeader(HttpServletRequest request, String externalId) {
    User user = users.findByExternalId(externalId).orElse(null);
    AuthContext.bindExternalId(request, externalId, user);
    bindUserLabel(user, externalId);
    if (user == null) {
      // 未建档是合法场景（首次登录要提交身份资料），但访问其他接口会被拒绝，这里提示清楚
      log.warn("[鉴权] X-User-Id=[{}] 尚未建立用户档案，除提交身份资料外的接口会被拒绝；可用演示账号 {}",
          Logs.abbrev(externalId, 12), DEMO_ACCOUNTS);
    } else {
      log.debug("[鉴权] 开发模式指定身份 externalId={} 已建档=true", Logs.abbrev(externalId));
    }
    return true;
  }

  /**
   * 开发模式兜底：未带任何有效凭证时使用默认演示账号。
   *
   * <p>账号不存在时只打 WARN 并保持未登录（接口会返回 401），
   * 避免把「种子数据没导进去」伪装成「权限不足」。
   */
  private boolean resolveByDevFallback(HttpServletRequest request) {
    String externalId = authProperties.devDefaultUser();
    User user = users.findByExternalId(externalId).orElse(null);
    if (user == null) {
      log.warn("[鉴权] 开发模式默认账号 [{}] 不存在，请先执行数据库迁移导入演示数据", externalId);
      return false;
    }
    AuthContext.bindExternalId(request, externalId, user);
    bindUserLabel(user, externalId);
    log.debug("[鉴权] 开发模式使用默认账号 externalId={}", externalId);
    return true;
  }

  /**
   * 判断请求头值是不是未解析的模板变量，例如 {@code {{userId}}}。
   *
   * <p>这些值不可能对应真实账号，当作「没传」处理比抛 401 更符合调试预期。
   */
  private boolean looksLikeTemplatePlaceholder(String value) {
    return value.contains("{{") || value.contains("}}");
  }

  /**
   * 把当前用户写进 MDC，后续业务日志会自动带上，便于按人排查。
   * 优先用学工号，没有档案时退化为 externalId 前缀。
   */
  private void bindUserLabel(User user, String externalId) {
    if (user != null && StringUtils.hasText(user.getEmployeeNo())) {
      MDC.put(TraceIdFilter.USER, user.getEmployeeNo());
    } else {
      MDC.put(TraceIdFilter.USER, Logs.abbrev(externalId, 6));
    }
  }
}
