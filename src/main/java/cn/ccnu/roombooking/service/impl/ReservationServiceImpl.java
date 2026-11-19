package cn.ccnu.roombooking.service.impl;

import cn.ccnu.roombooking.common.ApiException;
import cn.ccnu.roombooking.common.TimeUtils;
import cn.ccnu.roombooking.dto.request.ReservationCreateRequest;
import cn.ccnu.roombooking.dto.response.ReservationResponse;
import cn.ccnu.roombooking.entity.Closure;
import cn.ccnu.roombooking.entity.Reservation;
import cn.ccnu.roombooking.entity.Room;
import cn.ccnu.roombooking.entity.User;
import cn.ccnu.roombooking.enums.MemberType;
import cn.ccnu.roombooking.enums.NotificationType;
import cn.ccnu.roombooking.enums.ReservationStatus;
import cn.ccnu.roombooking.mapper.ClosureMapper;
import cn.ccnu.roombooking.mapper.ReservationMapper;
import cn.ccnu.roombooking.mapper.RoomMapper;
import cn.ccnu.roombooking.security.AuthContext;
import cn.ccnu.roombooking.service.AuditService;
import cn.ccnu.roombooking.service.ReservationLifecycleService;
import cn.ccnu.roombooking.service.ReservationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link ReservationService} 的默认实现：创建走行锁防超卖，查询与取消做权限判定。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

  /** 会占用时段的预约状态：其余状态视为已释放 */
  private static final List<ReservationStatus> ACTIVE_STATUSES =
      List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

  private final ReservationMapper reservations;
  private final RoomMapper rooms;
  private final ClosureMapper closures;
  private final AuthContext authContext;
  private final AuditService auditService;
  private final ReservationLifecycleService lifecycleService;

  @Override
  @Transactional
  public ReservationResponse create(ReservationCreateRequest request) {
    User applicant = authContext.approved();

    // 幂等：同一 requestId 重复提交（网络重试 / 用户连点）直接返回首次结果
    Reservation duplicated = reservations.selectOne(Wrappers.<Reservation>lambdaQuery()
        .eq(Reservation::getApplicantId, applicant.getId())
        .eq(Reservation::getRequestId, request.requestId()));
    if (duplicated != null) {
      log.debug("[预约创建] 幂等命中，直接返回首次结果 id={} requestId={}",
          duplicated.getId(), request.requestId());
      return ReservationResponse.of(duplicated, roomNoOf(duplicated.getRoomId()), false);
    }

    Room room = rooms.findByRoomNo(request.roomNo())
        .orElseThrow(() -> ApiException.notFound("ROOM_NOT_FOUND", "会议室不存在"));

    int startMinute = TimeUtils.toMinute(request.start());
    int endMinute = TimeUtils.toMinute(request.end());
    TimeUtils.validateBookable(request.date(), startMinute, endMinute);

    if (room.getCapacity() != null && request.attendeeCount() > room.getCapacity()) {
      log.warn("[预约创建] 人数超容量 room={} 容量={} 申请人数={}",
          room.getRoomNo(), room.getCapacity(), request.attendeeCount());
      throw ApiException.badRequest("CAPACITY_EXCEEDED", "预计人数超过会议室容量");
    }

    // 停用区间与申请时段只要有交集就不允许下单
    LocalDateTime from = TimeUtils.at(request.date(), startMinute);
    LocalDateTime to = TimeUtils.at(request.date(), endMinute);
    Long closedCount = closures.selectCount(Wrappers.<Closure>lambdaQuery()
        .eq(Closure::getRoomId, room.getId())
        .eq(Closure::isActive, true)
        .lt(Closure::getStartAt, to)
        .gt(Closure::getEndAt, from));
    if (closedCount != null && closedCount > 0) {
      log.warn("[预约创建] 时段已停用 room={} date={} {}-{}",
          room.getRoomNo(), request.date(), request.start(), request.end());
      throw ApiException.conflict("ROOM_CLOSED", "该时段已停用");
    }

    // 先结算当天已过期/已完成的预约，再抢锁校验冲突，防止把已失效的时段误判为占用
    lifecycleService.reconcile(room.getId(), request.date());
    boolean conflict = !reservations
        .selectOverlappingForUpdate(room.getId(), request.date(), startMinute, endMinute, ACTIVE_STATUSES)
        .isEmpty();
    if (conflict) {
      log.warn("[预约创建] 时段冲突 room={} date={} {}-{} 申请人={}",
          room.getRoomNo(), request.date(), request.start(), request.end(),
          applicant.getEmployeeNo());
      throw ApiException.conflict("TIME_CONFLICT", "该时段已被占用");
    }

    Reservation reservation = new Reservation();
    reservation.setRoomId(room.getId());
    reservation.setApplicantId(applicant.getId());
    reservation.setApplicantName(applicant.getName());
    reservation.setApplicantPhone(request.phone());
    reservation.setReservationDate(request.date());
    reservation.setStartMinute(startMinute);
    reservation.setEndMinute(endMinute);
    reservation.setReason(request.reason());
    reservation.setAttendeeCount(request.attendeeCount());
    reservation.setNote(request.note());
    reservation.setRequestId(request.requestId());
    // 学生需要负责人确认，教职工直接生效
    reservation.setStatus(applicant.getMemberType() == MemberType.STUDENT
        ? ReservationStatus.PENDING
        : ReservationStatus.CONFIRMED);
    reservations.insert(reservation);

    boolean pending = reservation.getStatus() == ReservationStatus.PENDING;
    auditService.notify(applicant.getId(),
        pending ? NotificationType.IDENTITY : NotificationType.APPROVED,
        reservation.getId(),
        pending ? "预约已提交，等待负责人确认" : "预约成功");

    log.info("[预约创建] 成功 id={} room={} date={} {}-{} 申请人={} 人数={} 状态={}",
        reservation.getId(), room.getRoomNo(), request.date(),
        TimeUtils.toText(startMinute), TimeUtils.toText(endMinute),
        applicant.getEmployeeNo(), request.attendeeCount(), reservation.getStatus());

    return ReservationResponse.of(reservation, room.getRoomNo(), false);
  }

  @Override
  public List<ReservationResponse> mine() {
    User applicant = authContext.approved();
    List<Reservation> list = reservations.selectList(Wrappers.<Reservation>lambdaQuery()
        .eq(Reservation::getApplicantId, applicant.getId())
        .orderByDesc(Reservation::getReservationDate)
        .orderByDesc(Reservation::getStartMinute));
    Map<UUID, String> roomNos = roomNumbers(list);
    log.debug("[预约查询] 我的预约共 {} 条 申请人={}", list.size(), applicant.getEmployeeNo());
    return list.stream()
        .map(reservation -> ReservationResponse.of(reservation,
            roomNos.get(reservation.getRoomId()), false))
        .toList();
  }

  @Override
  public ReservationResponse detail(UUID id) {
    User viewer = authContext.approved();
    Reservation reservation = requireReservation(id);
    boolean owner = reservation.getApplicantId().equals(viewer.getId());
    if (!owner && !viewer.isManager()) {
      log.warn("[预约详情] 越权访问被拒绝 id={} 访问人={}", id, viewer.getEmployeeNo());
      throw ApiException.forbidden("FORBIDDEN", "无权查看此预约");
    }
    // 先结算再重新读取，保证返回的状态与时间一致（MyBatis 没有一级缓存可用）
    lifecycleService.reconcile(reservation.getRoomId(), reservation.getReservationDate());
    Reservation fresh = reservations.selectById(id);
    return ReservationResponse.of(fresh, roomNoOf(fresh.getRoomId()), true);
  }

  @Override
  @Transactional
  public ReservationResponse cancel(UUID id, String reason) {
    User applicant = authContext.approved();
    Reservation reservation = requireReservation(id);
    if (!reservation.getApplicantId().equals(applicant.getId())) {
      throw ApiException.forbidden("FORBIDDEN", "只能取消本人预约");
    }
    // 终态预约重复取消直接返回，保证接口幂等
    if (reservation.getStatus() != ReservationStatus.PENDING
        && reservation.getStatus() != ReservationStatus.CONFIRMED) {
      log.debug("[预约取消] 幂等命中，预约已是终态 id={} 状态={}",
          reservation.getId(), reservation.getStatus());
      return ReservationResponse.of(reservation, roomNoOf(reservation.getRoomId()), true);
    }
    if (!TimeUtils.at(reservation.getReservationDate(), reservation.getStartMinute())
        .isAfter(LocalDateTime.now())) {
      log.warn("[预约取消] 已过开始时间，拒绝自助取消 id={} 申请人={}",
          reservation.getId(), applicant.getEmployeeNo());
      throw ApiException.conflict("ALREADY_STARTED", "开始后不能自助取消");
    }

    reservation.setStatus(ReservationStatus.CANCELLED);
    reservation.setResultReason(reason);
    reservations.updateById(reservation);

    auditService.notify(applicant.getId(), NotificationType.CANCELLED, reservation.getId(), "预约已取消");
    log.info("[预约取消] 成功 id={} date={} {}-{} 申请人={} 原因={}",
        reservation.getId(), reservation.getReservationDate(),
        TimeUtils.toText(reservation.getStartMinute()), TimeUtils.toText(reservation.getEndMinute()),
        applicant.getEmployeeNo(), reason == null ? "-" : reason);
    return ReservationResponse.of(reservation, roomNoOf(reservation.getRoomId()), true);
  }

  /** 按 ID 取预约，不存在时抛 404 */
  private Reservation requireReservation(UUID id) {
    Reservation reservation = reservations.selectById(id);
    if (reservation == null) {
      throw ApiException.notFound("RESERVATION_NOT_FOUND", "预约不存在");
    }
    return reservation;
  }

  /** 单条查询会议室编号，会议室被删除时返回 null */
  private String roomNoOf(UUID roomId) {
    Room room = rooms.selectById(roomId);
    return room == null ? null : room.getRoomNo();
  }

  /** 批量把会议室 ID 映射成编号，避免列表接口逐条查询 */
  private Map<UUID, String> roomNumbers(List<Reservation> list) {
    List<UUID> roomIds = list.stream().map(Reservation::getRoomId).distinct().toList();
    if (roomIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, String> index = new HashMap<>();
    rooms.selectBatchIds(roomIds).forEach(room -> index.put(room.getId(), room.getRoomNo()));
    return index;
  }
}
