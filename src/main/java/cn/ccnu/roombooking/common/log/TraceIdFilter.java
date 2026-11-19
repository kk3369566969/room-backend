package cn.ccnu.roombooking.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求追踪与访问日志过滤器。
 *
 * <p>为每个请求生成一个 8 位 traceId 写入 MDC，日志格式里会带出来，这样一次请求产生的
 * 所有日志（包括业务层、SQL 报错）都能用同一个 traceId 串起来排查。
 * 若客户端带了 {@code X-Trace-Id} 则沿用，便于前后端联调时对齐。
 *
 * <p>同一请求结束时按状态码分级输出一行访问日志：5xx 用 ERROR，4xx 用 WARN，其余 INFO，
 * 并附带耗时，方便定位慢接口与错误集中点。
 *
 * <p>执行顺序在所有业务过滤器之前（{@code AuthFilter} 是 HIGHEST_PRECEDENCE + 10），
 * 这样鉴权阶段打的日志也能带上 traceId。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceIdFilter extends OncePerRequestFilter {

  /** MDC 中 traceId 的键名，日志 pattern 里用 %X{traceId} 引用 */
  public static final String TRACE_ID = "traceId";
  /** MDC 中当前用户标识的键名，由 AuthFilter 解析登录态后写入 */
  public static final String USER = "user";
  /** 响应头回传 traceId，方便前端报错时直接贴给后端 */
  public static final String TRACE_HEADER = "X-Trace-Id";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
    String traceId = resolveTraceId(request.getHeader(TRACE_HEADER));
    MDC.put(TRACE_ID, traceId);
    response.setHeader(TRACE_HEADER, traceId);

    long startNanos = System.nanoTime();
    try {
      chain.doFilter(request, response);
    } finally {
      long costMillis = (System.nanoTime() - startNanos) / 1_000_000;
      logAccess(request, response.getStatus(), costMillis);
      MDC.remove(TRACE_ID);
      MDC.remove(USER);
    }
  }

  /** 客户端带了就用客户端的，否则本地生成，长度统一为 8 位 */
  private String resolveTraceId(String header) {
    if (StringUtils.hasText(header)) {
      return header.length() > 32 ? header.substring(0, 32) : header;
    }
    return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
  }

  /** 按状态码分级：5xx 错误、4xx 警告、其余信息 */
  private void logAccess(HttpServletRequest request, int status, long costMillis) {
    String method = request.getMethod();
    String uri = request.getRequestURI();
    String query = request.getQueryString();
    String target = query == null ? uri : uri + "?" + query;

    if (status >= 500) {
      log.error("[访问] {} {} -> {} ({}ms)", method, target, status, costMillis);
    } else if (status >= 400) {
      log.warn("[访问] {} {} -> {} ({}ms)", method, target, status, costMillis);
    } else if (log.isInfoEnabled()) {
      log.info("[访问] {} {} -> {} ({}ms)", method, target, status, costMillis);
    }
  }
}
