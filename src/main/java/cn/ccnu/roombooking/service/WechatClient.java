package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;

@Component
public class WechatClient {
  private final String appId;
  private final String secret;
  private final String endpoint;

  public WechatClient(
      @Value("${app.wechat.app-id:}") String appId,
      @Value("${app.wechat.secret:}") String secret,
      @Value("${app.wechat.jscode2session-url:https://api.weixin.qq.com/sns/jscode2session}") String endpoint) {
    this.appId = appId;
    this.secret = secret;
    this.endpoint = endpoint;
  }

  public String exchangeCode(String code) {
    if (appId.isBlank() || secret.isBlank()) {
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WECHAT_NOT_CONFIGURED", "服务端尚未配置微信小程序 appId 和 secret");
    }
    try {
      var uri = UriComponentsBuilder.fromUriString(endpoint)
          .queryParam("appid", appId)
          .queryParam("secret", secret)
          .queryParam("js_code", code)
          .queryParam("grant_type", "authorization_code")
          .build()
          .encode()
          .toUri();
      Map<?, ?> body = RestClient.create().get()
          .uri(uri)
          .retrieve()
          .body(Map.class);
      if (body == null) throw new ApiException(HttpStatus.BAD_GATEWAY, "WECHAT_LOGIN_FAILED", "微信登录服务未返回结果");
      Object errcode = body.get("errcode");
      if (errcode != null && !"0".equals(String.valueOf(errcode))) {
        throw new ApiException(HttpStatus.BAD_GATEWAY, "WECHAT_LOGIN_FAILED", "微信登录凭证无效或已过期");
      }
      Object openid = body.get("openid");
      if (openid == null || String.valueOf(openid).isBlank()) {
        throw new ApiException(HttpStatus.BAD_GATEWAY, "WECHAT_LOGIN_FAILED", "微信登录未返回 openid");
      }
      return String.valueOf(openid);
    } catch (ApiException e) {
      throw e;
    } catch (RestClientException e) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "WECHAT_UNAVAILABLE", "微信登录服务暂时不可用");
    }
  }
}
