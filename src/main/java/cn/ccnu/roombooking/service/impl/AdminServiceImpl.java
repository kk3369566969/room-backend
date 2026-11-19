package cn.ccnu.roombooking.service.impl;

import cn.ccnu.roombooking.common.ApiException;
import cn.ccnu.roombooking.common.TimeUtils;
import cn.ccnu.roombooking.dto.request.ClosureRequest;
import cn.ccnu.roombooking.dto.request.RoomUpdateRequest;
import cn.ccnu.roombooking.dto.response.ClosureResponse;
import cn.ccnu.roombooking.dto.response.OperationLogResponse;
import cn.ccnu.roombooking.dto.response.ReservationResponse;
import cn.ccnu.roombooking.dto.response.RoomResponse;
import cn.ccnu.roombooking.entity.Closure;
import cn.ccnu.roombooking.entity.OperationLog;
import cn.ccnu.roombooking.entity.Reservation;
import cn.ccnu.roombooking.entity.Room;
import cn.ccnu.roombooking.entity.User;
import cn.ccnu.roombooking.enums.NotificationType;
import cn.ccnu.roombooking.enums.ObjectType;
import cn.ccnu.roombooking.enums.ReservationStatus;
import cn.ccnu.roombooking.mapper.ClosureMapper;
import cn.ccnu.roombooking.mapper.OperationLogMapper;
import cn.ccnu.roombooking.mapper.ReservationMapper;
import cn.ccnu.roombooking.mapper.RoomMapper;
import cn.ccnu.roombooking.mapper.UserMapper;
import cn.ccnu.roombooking.security.AuthContext;
import cn.ccnu.roombooking.service.AdminService;
import cn.ccnu.roombooking.service.AuditService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** {@link AdminService} 的默认实现：负责人侧的审批、台账、房间资料、停用与日志。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

  /** 会占用时段的预约状态，审批与停用时用于判定影响范围 */
  private static final List<ReservationStatus> ACTIVE_STATUSES =
      List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

  /** 日志接口单页返回上限，够用且避免一次拉爆 */
  private static final int LOG_PAGE_SIZE = 100;

  private final AuthContext authContext;
  private final ReservationMapper reservations;
  private final RoomMapper rooms;
  private final ClosureMapper closures;
  private final OperationLogMapper operationLogs;
  private final UserMapper users;
  private final AuditService auditService;

  @Override
  @Transactional
  public ReservationResponse decide(UUID reservationId, boolean approve, String reason) {
    User manager = authContext.manager();
    Reservation reservation = reservations.selectById(reservationId);
    if (reservation == null) {
      throw ApiException.notFound("RESERVATION_NOT_FOUND", "预约不存在");
    }
    if (reservation.getStatus() != ReservationStatus.PENDING) {
      throw ApiException.conflict("RESERVATION_HANDLED", "预约已处理");
    }
    if (!approve && !StringUtils.hasText(reason)) {
      throw ApiException.badRequest("REASON_REQUIRED", "拒绝必须填写原因");
    }

    if (approve) {
      // 提交到审批之间可能已有人订走时段或房间缩容，通过前必须再校验一次
      boolean conflict = reservations.selectOverlappingForUpdate(reservation.getRoomId(),
              reservation.getReservationDate(), reservation.getStartMinute(),
              reservation.getEndMinute(), ACTIVE_STATUSES)
          .stream()
          .anyMatch(other -> !other.getId().equals(reservationId));
      if (conflict) {
        log.warn("[预约审批] 确认失败，时段已被占用 id={} roomId={} date={}",
            reservationId, reservation.getRoomId(), reservation.getReservationDate());
        throw ApiException.conflict("TIME_CONFLICT", "确认时段已被占用");
      }
      Room room = rooms.selectById(reservation.getRoomId());
      if (room != null && room.getCapacity() != null
          && reservation.getAttendeeCount() > room.getCapacity()) {
        log.warn("[预约审批] 确认失败，容量不足 id={} room={} 容量={} 人数={}",
            reservationId, room.getRoomNo(), room.getCapacity(), reservation.getAttendeeCount());
        throw ApiException.conflict("CAPACITY_EXCEEDED", "当前容量不足");
      }
      reservation.setStatus(ReservationStatus.CONFIRMED);
    } else {
      reservation.setStatus(ReservationStatus.REJECTED);
    }
    reservation.setHandledBy(manager.getId());
    reservation.setHandledAt(LocalDateTime.now());
    reservation.setResultReason(reason);
    reservations.updateById(reservation);

    auditService.notify(reservation.getApplicantId(),
        approve ? NotificationType.APPROVED : NotificationType.REJECTED,
        reservation.getId(),
        approve ? "学生预约已确认" : "学生预约被拒绝：" + reason);
    auditService.log(manager.getId(), ObjectType.RESERVATION, reservation.getId(),
        approve ? "APPROVE" : "REJECT", "SUCCESS", reason);

    log.info("[预约审批] {} id={} room={} date={} {}-{} 负责人={} 意见={}",
        approve ? "通过" : "拒绝", reservation.getId(),
        roomNoOf(reservation.getRoomId()), reservation.getReservationDate(),
        TimeUtils.toText(reservation.getStartMinute()), TimeUtils.toText(reservation.getEndMinute()),
        manager.getEmployeeNo(), reason == null ? "-" : reason);

    return ReservationResponse.of(reservation, roomNoOf(reservation.getRoomId()), true);
  }

  @Override
  public List<ReservationResponse> ledger(ReservationStatus status) {
    authContext.manager();
    List<Reservation> list = reservations.selectList(status == null
        ? null
        : Wrappers.<Reservation>lambdaQuery().eq(Reservation::getStatus, status));
    Map<UUID, String> roomNos = roomNumbers(list);
    return list.stream()
        .map(reservation -> ReservationResponse.of(reservation,
            roomNos.get(reservation.getRoomId()), true))
        .toList();
  }

  @Override
  @Transactional
  public RoomResponse updateRoom(String roomNo, RoomUpdateRequest request) {
    User manager = authContext.manager();
    Room room = requireRoom(roomNo);
    if (request.capacity() != null && request.capacity() <= 0) {
      throw ApiException.badRequest("INVALID_CAPACITY", "容量必须为正整数");
    }
    // 逐字段判空：null 表示本次不修改，支持前端只提交变化的部分
    if (request.capacity() != null) {
      room.setCapacity(request.capacity());
    }
    if (request.equipment() != null) {
      room.setEquipment(request.equipment());
    }
    if (request.restrictionText() != null) {
      room.setRestrictionText(request.restrictionText());
    }
    if (request.location() != null) {
      room.setLocation(request.location());
    }
    rooms.updateById(room);

    auditService.log(manager.getId(), ObjectType.ROOM, room.getId(), "UPDATE", "SUCCESS", "更新房间资料");
    log.info("[房间更新] roomNo={} 容量={} 设备={} 位置={} 负责人={}",
        room.getRoomNo(), room.getCapacity(), room.getEquipment(), room.getLocation(),
        manager.getEmployeeNo());
    return RoomResponse.of(room);
  }

  @Override
  public List<ReservationResponse> closurePreview(ClosureRequest request) {
    authContext.manager();
    Room room = requireRoom(request.roomNo());
    List<Reservation> affected = affectedReservations(room, request);
    log.debug("[停用预览] room={} {}~{} 命中预约 {} 条",
        room.getRoomNo(), request.startAt(), request.endAt(), affected.size());
    return affected.stream()
        .map(reservation -> ReservationResponse.of(reservation, room.getRoomNo(), true))
        .toList();
  }

  @Override
  @Transactional
  public UUID createClosure(ClosureRequest request) {
    User manager = authContext.manager();
    // confirm 是「已看过预览」的显式确认，避免误操作一次性取消一批预约
    if (!Boolean.TRUE.equals(request.confirm())) {
      throw ApiException.badRequest("CONFIRM_REQUIRED", "停用需要确认影响范围");
    }
    Room room = requireRoom(request.roomNo());
    List<Reservation> affected = affectedReservations(room, request);

    Closure closure = new Closure();
    closure.setRoomId(room.getId());
    closure.setStartAt(request.startAt());
    closure.setEndAt(request.endAt());
    closure.setReason(request.reason());
    closure.setManagerId(manager.getId());
    closure.setActive(true);
    closures.insert(closure);

    // 受影响的预约逐条置为已取消，并通知各自的申请人
    for (Reservation reservation : affected) {
      reservation.setStatus(ReservationStatus.CANCELLED);
      reservation.setResultReason("会议室停用：" + request.reason());
      reservation.setHandledBy(manager.getId());
      reservation.setHandledAt(LocalDateTime.now());
      reservations.updateById(reservation);
      auditService.notify(reservation.getApplicantId(), NotificationType.CLOSURE,
          reservation.getId(), "预约因会议室停用被取消：" + request.reason());
    }

    auditService.log(manager.getId(), ObjectType.CLOSURE, closure.getId(), "CREATE",
        "SUCCESS", "影响预约数=" + affected.size());
    // 一次停用可能批量取消预约，用 WARN 记一条，便于事后追溯影响面
    log.warn("[停用创建] closureId={} room={} {}~{} 原因={} 取消预约={} 条 负责人={}",
        closure.getId(), room.getRoomNo(), request.startAt(), request.endAt(), request.reason(),
        affected.size(), manager.getEmployeeNo());
    return closure.getId();
  }

  @Override
  @Transactional
  public ClosureResponse releaseClosure(UUID closureId) {
    User manager = authContext.manager();
    Closure closure = closures.selectById(closureId);
    if (closure == null) {
      throw ApiException.notFound("CLOSURE_NOT_FOUND", "停用不存在");
    }
    // 只解除停用，不恢复已被取消的预约——那批用户需要重新走预约流程
    closure.setActive(false);
    closures.updateById(closure);
    auditService.log(manager.getId(), ObjectType.CLOSURE, closure.getId(), "RELEASE",
        "SUCCESS", "解除停用");
    log.info("[停用解除] closureId={} roomId={} 负责人={}",
        closure.getId(), closure.getRoomId(), manager.getEmployeeNo());

    return ClosureResponse.of(closure,
        rooms.selectById(closure.getRoomId()),
        users.selectById(closure.getManagerId()));
  }

  @Override
  public List<OperationLogResponse> logs() {
    authContext.manager();
    Page<OperationLog> page = operationLogs.selectPage(new Page<>(1, LOG_PAGE_SIZE),
        Wrappers.<OperationLog>lambdaQuery().orderByDesc(OperationLog::getCreatedAt));
    Map<UUID, User> operators = operators(page.getRecords());
    return page.getRecords().stream()
        .map(log -> OperationLogResponse.of(log, operators.get(log.getOperatorId())))
        .toList();
  }

  /** 与停用区间重叠、且会被取消的预约 */
  private List<Reservation> affectedReservations(Room room, ClosureRequest request) {
    if (!request.endAt().isAfter(request.startAt())) {
      throw ApiException.badRequest("INVALID_INTERVAL", "停用结束必须晚于开始");
    }
    // 先按日期粗筛，再按分钟级时段精确判定，避免跨天比较出错
    LocalDate fromDate = request.startAt().toLocalDate();
    LocalDate toDate = request.endAt().toLocalDate();
    return reservations.selectList(Wrappers.<Reservation>lambdaQuery()
            .eq(Reservation::getRoomId, room.getId())
            .in(Reservation::getStatus, ACTIVE_STATUSES)
            .between(Reservation::getReservationDate, fromDate, toDate))
        .stream()
        .filter(reservation -> overlaps(reservation, request))
        .toList();
  }

  /** 预约时段与停用区间是否有交集 */
  private boolean overlaps(Reservation reservation, ClosureRequest request) {
    LocalDateTime start = TimeUtils.at(reservation.getReservationDate(), reservation.getStartMinute());
    LocalDateTime end = TimeUtils.at(reservation.getReservationDate(), reservation.getEndMinute());
    return start.isBefore(request.endAt()) && end.isAfter(request.startAt());
  }

  /** 按编号取会议室，不存在时抛 404 */
  private Room requireRoom(String roomNo) {
    return rooms.findByRoomNo(roomNo)
        .orElseThrow(() -> ApiException.notFound("ROOM_NOT_FOUND", "会议室不存在"));
  }

  /** 单条查询会议室编号，会议室被删除时返回 null */
  private String roomNoOf(UUID roomId) {
    Room room = rooms.selectById(roomId);
    return room == null ? null : room.getRoomNo();
  }

  /** 批量把会议室 ID 映射成编号，避免台账逐条查询 */
  private Map<UUID, String> roomNumbers(List<Reservation> list) {
    List<UUID> roomIds = list.stream().map(Reservation::getRoomId).distinct().toList();
    if (roomIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, String> index = new HashMap<>();
    rooms.selectBatchIds(roomIds).forEach(room -> index.put(room.getId(), room.getRoomNo()));
    return index;
  }

  /** 批量加载日志里的操作人，避免逐条查询造成 N+1 */
  private Map<UUID, User> operators(List<OperationLog> list) {
    List<UUID> ids = list.stream()
        .map(OperationLog::getOperatorId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<UUID, User> index = new HashMap<>();
    users.selectBatchIds(ids).forEach(user -> index.put(user.getId(), user));
    return index;
  }
}
