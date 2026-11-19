package cn.ccnu.roombooking.client;

import cn.ccnu.roombooking.common.ApiException;
import cn.ccnu.roombooking.config.properties.WechatProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 微信小程序登录客户端：用 code 换取 openid。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatClient {

  private final WechatProperties properties;
  private final RestClient restClient = RestClient.create();

  public String exchangeCode(String code) {
    if (!properties.configured()) {
      log.error("[微信登录] 服务端未配置 appId/secret，无法换取 openid");
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WECHAT_NOT_CONFIGURED",
          "服务端尚未配置微信小程序 appId 和 secret");
    }
    try {
      var uri = UriComponentsBuilder.fromUriString(properties.jscode2sessionUrl())
          .queryParam("appid", properties.appId())
          .queryParam("secret", properties.secret())
          .queryParam("js_code", code)
          .queryParam("grant_type", "authorization_code")
          .build()
          .encode()
          .toUri();
      Map<?, ?> body = restClient.get().uri(uri).retrieve().body(Map.class);
      if (body == null) {
        log.warn("[微信登录] 微信接口返回空响应");
        throw new ApiException(HttpStatus.BAD_GATEWAY, "WECHAT_LOGIN_FAILED", "微信登录服务未返回结果");
      }
      Object errcode = body.get("errcode");
      if (errcode != null && !"0".equals(String.valueOf(errcode))) {
        // errcode 是微信侧的业务码，对排查「code 已用过 / 过期」很关键，需保留
        log.warn("[微信登录] 微信接口返回错误 errcode={} errmsg={}",
            errcode, body.get("errmsg"));
        throw new ApiException(HttpStatus.BAD_GATEWAY, "WECHAT_LOGIN_FAILED", "微信登录凭证无效或已过期");
      }
      Object openid = body.get("openid");
      if (openid == null || String.valueOf(openid).isBlank()) {
        log.warn("[微信登录] 微信接口未返回 openid");
        throw new ApiException(HttpStatus.BAD_GATEWAY, "WECHAT_LOGIN_FAILED", "微信登录未返回 openid");
      }
      log.debug("[微信登录] code 换取 openid 成功");
      return String.valueOf(openid);
    } catch (ApiException e) {
      throw e;
    } catch (RestClientException e) {
      log.error("[微信登录] 调用微信接口失败", e);
      throw new ApiException(HttpStatus.BAD_GATEWAY, "WECHAT_UNAVAILABLE", "微信登录服务暂时不可用");
    }
  }
}
