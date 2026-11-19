package cn.ccnu.roombooking.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 预约状态机服务。
 *
 * <p>把「到点仍未确认」的预约置为失效、把「已结束」的预约置为完成。
 * 采用惰性结算：在读取某天日程或预约详情前调用，避免依赖定时任务，
 * 也避免在没有请求时无谓地扫表。
 *
 * <p>实现类：{@link cn.ccnu.roombooking.service.impl.ReservationLifecycleServiceImpl}
 */
public interface ReservationLifecycleService {

  /**
   * 结算指定会议室某一天的预约状态。
   *
   * <p>规则：PENDING 且已到开始时间 → EXPIRED（并通知申请人）；
   * CONFIRMED 且已过结束时间 → COMPLETED。已处于终态的记录不受影响。
   * 方法可重复调用，第二次调用通常不再产生更新。
   *
   * @param roomId 会议室 ID
   * @param date   需要结算的日期，调用方应传入「当前所查询的那一天」
   */
  void reconcile(UUID roomId, LocalDate date);
}
