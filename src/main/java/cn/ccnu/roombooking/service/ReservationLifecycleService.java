package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
public class ReservationLifecycleService {
  private final ReservationRepository reservations; private final AuditService audit;
  public ReservationLifecycleService(ReservationRepository reservations, AuditService audit) { this.reservations=reservations; this.audit=audit; }
  @Transactional public void reconcile(Room room, LocalDate date) { var now=LocalDateTime.now(); for (var r: reservations.findActiveBetween(room,date,date,java.util.List.of(Enums.ReservationStatus.PENDING,Enums.ReservationStatus.CONFIRMED))) { var start=TimeSupport.at(r.getReservationDate(),r.getStartMinute()); var end=TimeSupport.at(r.getReservationDate(),r.getEndMinute()); if (r.getStatus()==Enums.ReservationStatus.PENDING && !now.isBefore(start)) { r.setStatus(Enums.ReservationStatus.EXPIRED); r.setUpdatedAt(now); reservations.save(r); audit.notify(r.getApplicant(),Enums.NotificationType.EXPIRED,r.getId(),"预约到开始时间仍未确认，已失效"); } else if (r.getStatus()==Enums.ReservationStatus.CONFIRMED && !now.isBefore(end)) { r.setStatus(Enums.ReservationStatus.COMPLETED); r.setUpdatedAt(now); reservations.save(r); } } }
}
