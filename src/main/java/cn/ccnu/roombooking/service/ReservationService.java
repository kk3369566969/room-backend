package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.dto.request.ReservationCreateRequest;
import cn.ccnu.roombooking.dto.response.ReservationResponse;
import java.util.List;
import java.util.UUID;

/**
 * 预约的创建、查询与自助取消服务。
 *
 * <p>创建预约时按身份区分：学生提交后为 PENDING，需负责人确认；教职工直接为 CONFIRMED。
 * 同一时段只允许存在一条占用中的预约，并发创建通过数据库行锁保证互斥。
 *
 * <p>实现类：{@link cn.ccnu.roombooking.service.impl.ReservationServiceImpl}
 */
public interface ReservationService {

  /**
   * 创建一条预约。
   *
   * <p>校验顺序：幂等去重 → 会议室存在 → 时段是否在开放时间内且为整刻 → 人数不超过容量
   * → 该时段未被停用 → 无时段冲突（行锁）。校验通过后按身份决定初始状态并投递通知。
   *
   * @param request 会议室编号、日期、起止时间、事由、预计人数、备注与客户端幂等号
   * @return 新建或幂等命中的预约详情（含是否返回完整信息）
   * @throws cn.ccnu.roombooking.common.ApiException 会议室不存在（ROOM_NOT_FOUND）、
   *                                                 人数超容量（CAPACITY_EXCEEDED）、
   *                                                 时段已停用（ROOM_CLOSED）、
   *                                                 时段被占用（TIME_CONFLICT）、
   *                                                 时间格式非法（INVALID_TIME）、
   *                                                 不在开放时段（INVALID_INTERVAL）、
   *                                                 开始时间已过（START_IN_PAST）、
   *                                                 身份未通过审核（IDENTITY_REQUIRED）时抛出
   */
  ReservationResponse create(ReservationCreateRequest request);

  /**
   * 查询当前登录用户提交过的全部预约，按日期与开始时间倒序排列。
   *
   * @return 本人预约列表
   * @throws cn.ccnu.roombooking.common.ApiException 身份未通过审核（IDENTITY_REQUIRED）时抛出
   */
  List<ReservationResponse> mine();

  /**
   * 查询预约详情。
   *
   * <p>仅本人或负责人可查看；读取前会先对当天记录做一次状态结算，
   * 保证返回的状态不是过期缓存。
   *
   * @param id 预约 ID
   * @return 预约详情，包含完整信息（含事由、备注、处理意见）
   * @throws cn.ccnu.roombooking.common.ApiException 预约不存在（RESERVATION_NOT_FOUND）、
   *                                                 非本人且非负责人（FORBIDDEN）时抛出
   */
  ReservationResponse detail(UUID id);

  /**
   * 申请人自助取消预约。
   *
   * <p>只能取消本人预约，且必须在开始时间之前操作。对已处于终态（已取消 / 已拒绝 / 已失效 / 已完成）
   * 的预约重复取消时直接返回当前结果，不再报错，保证接口幂等。
   *
   * @param id     预约 ID
   * @param reason 取消原因，可为 null
   * @return 取消后的预约详情
   * @throws cn.ccnu.roombooking.common.ApiException 预约不存在（RESERVATION_NOT_FOUND）、
   *                                                 非本人预约（FORBIDDEN）、
   *                                                 已过开始时间（ALREADY_STARTED）时抛出
   */
  ReservationResponse cancel(UUID id, String reason);
}
