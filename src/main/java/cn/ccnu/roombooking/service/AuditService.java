package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.enums.NotificationType;
import cn.ccnu.roombooking.enums.ObjectType;
import java.util.UUID;

/**
 * 站内通知与操作日志的统一写入口。
 *
 * <p>业务代码不直接操作通知表和日志表，一律经由本服务落库，便于后续统一加工
 * （例如推送、清理策略、敏感信息脱敏）。两个方法都是「只写不读」的旁路动作，
 * 不参与调用方的事务回滚判断。
 *
 * <p>实现类：{@link cn.ccnu.roombooking.service.impl.AuditServiceImpl}
 */
public interface AuditService {

  /**
   * 向指定用户投递一条站内通知。
   *
   * @param recipientId 接收人用户 ID，通常是预约申请人或身份审核的提交人
   * @param type        通知类型，决定前端展示的图标与文案分组
   * @param businessId  关联业务对象 ID（预约 ID 或审核记录 ID），用于前端跳转定位
   * @param content     通知正文，直接展示给用户，调用方需保证内容可读
   */
  void notify(UUID recipientId, NotificationType type, UUID businessId, String content);

  /**
   * 记录一条负责人操作日志。
   *
   * <p>用于后台审计：谁、对哪个对象、做了什么、结果如何。仅记录负责人侧动作，
   * 普通用户的预约、取消不计入操作日志。
   *
   * @param operatorId 操作人（负责人）用户 ID
   * @param objectType 操作对象类型，如预约、房间、停用、审核
   * @param objectId   操作对象 ID
   * @param action     动作标识，如 APPROVE、REJECT、UPDATE、CREATE、RELEASE
   * @param result     执行结果，成功时固定为 SUCCESS
   * @param summary    操作摘要，可填拒绝原因、影响预约数等补充说明，允许为 null
   */
  void log(UUID operatorId, ObjectType objectType, UUID objectId, String action,
           String result, String summary);
}
