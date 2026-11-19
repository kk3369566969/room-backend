package cn.ccnu.roombooking.service.impl;

import cn.ccnu.roombooking.common.ApiException;
import cn.ccnu.roombooking.common.TimeUtils;
import cn.ccnu.roombooking.dto.response.RoomResponse;
import cn.ccnu.roombooking.dto.response.ScheduleItem;
import cn.ccnu.roombooking.entity.Closure;
import cn.ccnu.roombooking.entity.Reservation;
import cn.ccnu.roombooking.entity.Room;
import cn.ccnu.roombooking.entity.User;
import cn.ccnu.roombooking.enums.ReservationStatus;
import cn.ccnu.roombooking.mapper.ClosureMapper;
import cn.ccnu.roombooking.mapper.ReservationMapper;
import cn.ccnu.roombooking.mapper.RoomMapper;
import cn.ccnu.roombooking.security.AuthContext;
import cn.ccnu.roombooking.service.ReservationLifecycleService;
import cn.ccnu.roombooking.service.RoomService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** {@link RoomService} 的默认实现：会议室资料与日程查询。 */
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

  private final RoomMapper rooms;
  private final ReservationMapper reservations;
  private final ClosureMapper closures;
  private final AuthContext authContext;
  private final ReservationLifecycleService lifecycleService;

  @Override
  public List<RoomResponse> list() {
    authContext.approved();
    return rooms.selectList(null).stream().map(RoomResponse::of).toList();
  }

  @Override
  public RoomResponse get(String roomNo) {
    authContext.approved();
    return RoomResponse.of(requireRoom(roomNo));
  }

  @Override
  public List<ScheduleItem> schedule(String roomNo, LocalDate date) {
    User viewer = authContext.approved();
    Room room = requireRoom(roomNo);

    // 先结算，否则已过期但状态还是 PENDING 的记录会继续占着时段
    lifecycleService.reconcile(room.getId(), date);

    List<ScheduleItem> items = new ArrayList<>();
    List<Reservation> booked = reservations.selectList(Wrappers.<Reservation>lambdaQuery()
        .eq(Reservation::getRoomId, room.getId())
        .eq(Reservation::getReservationDate, date)
        .in(Reservation::getStatus,
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)));
    for (Reservation reservation : booked) {
      boolean own = reservation.getApplicantId().equals(viewer.getId());
      // 他人预约不暴露预约号，只给占用时段，避免被拿来猜测或试探接口
      items.add(new ScheduleItem(
          own ? reservation.getId().toString() : null,
          TimeUtils.toText(reservation.getStartMinute()),
          TimeUtils.toText(reservation.getEndMinute()),
          reservation.getStatus().name(),
          own));
    }

    // 停用区间与当天开放时段求交集后才返回
    LocalDateTime dayStart = TimeUtils.at(date, TimeUtils.OPEN_MINUTE);
    LocalDateTime dayEnd = TimeUtils.at(date, TimeUtils.CLOSE_MINUTE);
    List<Closure> closed = closures.selectList(Wrappers.<Closure>lambdaQuery()
        .eq(Closure::getRoomId, room.getId())
        .eq(Closure::isActive, true)
        .lt(Closure::getStartAt, dayEnd)
        .gt(Closure::getEndAt, dayStart)
        .orderByAsc(Closure::getStartAt));
    for (Closure closure : closed) {
      items.add(new ScheduleItem(
          null,
          closure.getStartAt().toLocalTime().toString(),
          closure.getEndAt().toLocalTime().toString(),
          "CLOSED",
          false));
    }
    return items;
  }

  /** 按编号取会议室，不存在时抛 404 */
  private Room requireRoom(String roomNo) {
    return rooms.findByRoomNo(roomNo)
        .orElseThrow(() -> ApiException.notFound("ROOM_NOT_FOUND", "会议室不存在"));
  }
}
