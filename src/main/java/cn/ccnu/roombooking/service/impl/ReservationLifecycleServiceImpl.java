package cn.ccnu.roombooking.service.impl;

import cn.ccnu.roombooking.common.TimeUtils;
import cn.ccnu.roombooking.entity.Reservation;
import cn.ccnu.roombooking.enums.NotificationType;
import cn.ccnu.roombooking.enums.ReservationStatus;
import cn.ccnu.roombooking.mapper.ReservationMapper;
import cn.ccnu.roombooking.service.AuditService;
import cn.ccnu.roombooking.service.ReservationLifecycleService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link ReservationLifecycleService} 的默认实现：读写前惰性结算当天的预约状态。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationLifecycleServiceImpl implements ReservationLifecycleService {

  private final ReservationMapper reservations;
  private final AuditService auditService;

  @Override
  @Transactional
  public void reconcile(UUID roomId, LocalDate date) {
    List<Reservation> active = reservations.selectList(Wrappers.<Reservation>lambdaQuery()
        .eq(Reservation::getRoomId, roomId)
        .eq(Reservation::getReservationDate, date)
        .in(Reservation::getStatus,
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)));

    LocalDateTime now = LocalDateTime.now();
    for (Reservation reservation : active) {
      LocalDateTime start = TimeUtils.at(date, reservation.getStartMinute());
      LocalDateTime end = TimeUtils.at(date, reservation.getEndMinute());

      if (reservation.getStatus() == ReservationStatus.PENDING && !now.isBefore(start)) {
        // 到点仍未确认：失效并告知申请人，避免把时段一直占着
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservations.updateById(reservation);
        auditService.notify(reservation.getApplicantId(), NotificationType.EXPIRED,
            reservation.getId(), "预约到开始时间仍未确认，已失效");
        log.info("[状态结算] 预约失效 id={} roomId={} date={} 开始={}",
            reservation.getId(), roomId, date, TimeUtils.toText(reservation.getStartMinute()));
      } else if (reservation.getStatus() == ReservationStatus.CONFIRMED && !now.isBefore(end)) {
        // 已过结束时间：静默置为完成，不打扰用户
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservations.updateById(reservation);
        log.info("[状态结算] 预约完成 id={} roomId={} date={} 结束={}",
            reservation.getId(), roomId, date, TimeUtils.toText(reservation.getEndMinute()));
      }
    }
  }
}
