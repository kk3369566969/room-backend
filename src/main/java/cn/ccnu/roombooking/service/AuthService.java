package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.dto.response.LoginResponse;

/**
 * 登录鉴权服务。
 *
 * <p>负责微信小程序登录凭证换取系统会话令牌，以及退出登录时吊销当前会话。
 * 会话令牌只以摘要形式落库，原始令牌仅在下发时出现一次。
 *
 * <p>实现类：{@link cn.ccnu.roombooking.service.impl.AuthServiceImpl}
 */
public interface AuthService {

  /**
   * 使用微信登录凭证换取系统会话令牌。
   *
   * <p>流程：向微信接口换取 externalId → 生成随机令牌并落库（不保存明文，仅存 SHA-256 摘要）
   * → 按 externalId 判断该用户是否已建立身份档案，把结果一并返回给前端以决定后续引导页。
   *
   * @param code 微信小程序 wx.login 返回的一次性 code，由微信侧校验并换取 openid
   * @return 会话令牌、有效期、externalId 以及用户档案的建立情况与身份状态
   * @throws cn.ccnu.roombooking.common.ApiException 微信凭证无效（WECHAT_LOGIN_FAILED）时抛出
   */
  LoginResponse login(String code);

  /**
   * 退出登录，吊销当前请求携带的会话。
   *
   * <p>把会话的吊销时间置为当前时间；已吊销或已过期的会话重复调用不报错，保证幂等。
   *
   * @throws cn.ccnu.roombooking.common.ApiException 当前请求没有有效会话（UNAUTHORIZED）时抛出
   */
  void logout();
}
