package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.dto.request.ClosureRequest;
import cn.ccnu.roombooking.dto.request.RoomUpdateRequest;
import cn.ccnu.roombooking.dto.response.ClosureResponse;
import cn.ccnu.roombooking.dto.response.OperationLogResponse;
import cn.ccnu.roombooking.dto.response.ReservationResponse;
import cn.ccnu.roombooking.dto.response.RoomResponse;
import cn.ccnu.roombooking.enums.ReservationStatus;
import java.util.List;
import java.util.UUID;

/**
 * 负责人后台服务：预约审批、台账、会议室资料维护、停用与操作日志。
 *
 * <p>所有方法都要求调用者具备负责人权限；除查询类方法外，写操作都会同步写入操作日志，
 * 需要通知申请人的场景还会投递站内通知。
 *
 * <p>实现类：{@link cn.ccnu.roombooking.service.impl.AdminServiceImpl}
 */
public interface AdminService {

  /**
   * 审批一条待确认的学生预约。
   *
   * <p>通过前会再次校验时段冲突与容量，避免「提交时可用、审批时已被占」的情况；
   * 拒绝时必须填写原因。处理完成后通知申请人并记录操作日志。
   *
   * @param reservationId 预约 ID
   * @param approve       true 表示确认通过，false 表示拒绝
   * @param reason        审批意见；拒绝时必填，通过时可为 null
   * @return 处理后的预约详情
   * @throws cn.ccnu.roombooking.common.ApiException 预约不存在（RESERVATION_NOT_FOUND）、
   *                                                 已处理过（RESERVATION_HANDLED）、
   *                                                 拒绝未填原因（REASON_REQUIRED）、
   *                                                 时段已被占用（TIME_CONFLICT）、
   *                                                 容量不足（CAPACITY_EXCEEDED）、
   *                                                 非负责人（MANAGER_REQUIRED）时抛出
   */
  ReservationResponse decide(UUID reservationId, boolean approve, String reason);

  /**
   * 查询预约台账，可按状态过滤。
   *
   * @param status 预约状态，传 null 表示查询全部
   * @return 预约列表，按数据库返回顺序排列，包含完整信息
   * @throws cn.ccnu.roombooking.common.ApiException 非负责人（MANAGER_REQUIRED）时抛出
   */
  List<ReservationResponse> ledger(ReservationStatus status);

  /**
   * 更新会议室资料，只覆盖请求中非空的字段（局部更新）。
   *
   * @param roomNo  会议室编号
   * @param request 容量、设备、使用限制、位置，均可为 null 表示不修改
   * @return 更新后的会议室资料
   * @throws cn.ccnu.roombooking.common.ApiException 会议室不存在（ROOM_NOT_FOUND）、
   *                                                 容量非正整数（INVALID_CAPACITY）、
   *                                                 非负责人（MANAGER_REQUIRED）时抛出
   */
  RoomResponse updateRoom(String roomNo, RoomUpdateRequest request);

  /**
   * 停用影响范围预览：列出该时段内将被取消的预约，供负责人在确认前核对。
   *
   * <p>只读，不产生任何数据变更。
   *
   * @param request 会议室编号与停用起止时间
   * @return 会被取消的预约列表
   * @throws cn.ccnu.roombooking.common.ApiException 会议室不存在（ROOM_NOT_FOUND）、
   *                                                 结束时间不晚于开始时间（INVALID_INTERVAL）、
   *                                                 非负责人（MANAGER_REQUIRED）时抛出
   */
  List<ReservationResponse> closurePreview(ClosureRequest request);

  /**
   * 创建会议室停用区间，并自动取消区间内所有占用中的预约。
   *
   * <p>必须带上 confirm=true，用于强制前端先调用过 {@link #closurePreview} 并让用户确认影响范围。
   * 被取消的预约逐条通知申请人，最后记录一条包含影响预约数的操作日志。
   *
   * @param request 会议室编号、停用起止时间、停用原因与确认标记
   * @return 新建的停用区间 ID
   * @throws cn.ccnu.roombooking.common.ApiException 未确认影响范围（CONFIRM_REQUIRED）、
   *                                                 会议室不存在（ROOM_NOT_FOUND）、
   *                                                 结束时间不晚于开始时间（INVALID_INTERVAL）、
   *                                                 非负责人（MANAGER_REQUIRED）时抛出
   */
  UUID createClosure(ClosureRequest request);

  /**
   * 解除停用，恢复会议室的正常预约。
   *
   * <p>只把停用区间置为失效，不会自动恢复此前被取消的预约——申请人需要重新预约。
   *
   * @param closureId 停用区间 ID
   * @return 解除后的停用区间详情
   * @throws cn.ccnu.roombooking.common.ApiException 停用记录不存在（CLOSURE_NOT_FOUND）、
   *                                                 非负责人（MANAGER_REQUIRED）时抛出
   */
  ClosureResponse releaseClosure(UUID closureId);

  /**
   * 查询最近的操作日志（按时间倒序，最多 100 条）。
   *
   * @return 操作日志列表，携带操作人信息
   * @throws cn.ccnu.roombooking.common.ApiException 非负责人（MANAGER_REQUIRED）时抛出
   */
  List<OperationLogResponse> logs();
}
