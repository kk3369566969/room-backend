package cn.ccnu.roombooking.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 登录会话与开发身份配置。
 *
 * @param sessionTtlSeconds 会话令牌有效期（秒）
 * @param devMode           开发模式开关：开启后允许用 X-User-Id 指定身份，
 *                          且未带任何凭证时自动使用 {@code devDefaultUser} 对应的账号
 * @param devDefaultUser    开发模式的默认身份，取 app_user.external_id
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
    @DefaultValue("604800") long sessionTtlSeconds,
    @DefaultValue("true") boolean devMode,
    @DefaultValue("manager-demo") String devDefaultUser) {
}
