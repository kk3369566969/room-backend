package cn.ccnu.roombooking.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 微信小程序登录相关配置 */
@ConfigurationProperties(prefix = "app.wechat")
public record WechatProperties(String appId, String secret, String jscode2sessionUrl) {

  public boolean configured() {
    return appId != null && !appId.isBlank() && secret != null && !secret.isBlank();
  }
}
