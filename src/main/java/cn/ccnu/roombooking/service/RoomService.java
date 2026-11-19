package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.dto.response.RoomResponse;
import cn.ccnu.roombooking.dto.response.ScheduleItem;
import java.time.LocalDate;
import java.util.List;

/**
 * 会议室资料与日程查询服务。
 *
 * <p>只负责读：会议室列表、单间资料、某天的占用情况。所有查询都要求身份已通过审核。
 *
 * <p>实现类：{@link cn.ccnu.roombooking.service.impl.RoomServiceImpl}
 */
public interface RoomService {

  /**
   * 查询全部会议室。
   *
   * @return 会议室列表（容量、设备、位置、使用限制等资料）
   * @throws cn.ccnu.roombooking.common.ApiException 身份未通过审核（IDENTITY_REQUIRED）时抛出
   */
  List<RoomResponse> list();

  /**
   * 按编号查询单个会议室的资料。
   *
   * @param roomNo 会议室编号，如 7081
   * @return 会议室资料
   * @throws cn.ccnu.roombooking.common.ApiException 会议室不存在（ROOM_NOT_FOUND）、
   *                                                 身份未通过审核（IDENTITY_REQUIRED）时抛出
   */
  RoomResponse get(String roomNo);

  /**
   * 查询某间会议室某一天的日程，包含已占用时段与停用时段。
   *
   * <p>隐私约束：本人的预约返回 reservationId，他人的预约只返回占用时段而不暴露预约号，
   * 前端据此渲染「可预约 / 已占用 / 本人预约 / 停用」四种色块。
   * 读取前会先结算当天的过期与完成状态，保证时段状态准确。
   *
   * @param roomNo 会议室编号
   * @param date   查询日期
   * @return 日程条目列表，按开始时间排列
   * @throws cn.ccnu.roombooking.common.ApiException 会议室不存在（ROOM_NOT_FOUND）、
   *                                                 身份未通过审核（IDENTITY_REQUIRED）时抛出
   */
  List<ScheduleItem> schedule(String roomNo, LocalDate date);
}
