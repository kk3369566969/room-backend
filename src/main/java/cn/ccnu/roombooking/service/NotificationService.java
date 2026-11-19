package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.dto.response.NotificationResponse;
import java.util.List;
import java.util.UUID;

/**
 * 站内通知查询与已读标记服务。
 *
 * <p>通知由 {@link AuditService#notify} 在业务事件发生时写入，这里只负责读取与标记已读。
 *
 * <p>实现类：{@link cn.ccnu.roombooking.service.impl.NotificationServiceImpl}
 */
public interface NotificationService {

  /**
   * 查询当前用户收到的全部通知，按创建时间倒序（最新的在最前）。
   *
   * @return 通知列表，未读通知的 readAt 为 null
   * @throws cn.ccnu.roombooking.common.ApiException 身份未通过审核（IDENTITY_REQUIRED）时抛出
   */
  List<NotificationResponse> listMine();

  /**
   * 把指定通知标记为已读。
   *
   * <p>重复标记不报错，只会刷新读取时间。
   *
   * @param id 通知 ID
   * @return 标记后的通知
   * @throws cn.ccnu.roombooking.common.ApiException 通知不存在（NOTIFICATION_NOT_FOUND）、
   *                                                 非本人通知（FORBIDDEN）时抛出
   */
  NotificationResponse markRead(UUID id);
}
