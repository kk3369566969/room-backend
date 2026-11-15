package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.common.*;
import cn.ccnu.roombooking.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class ReservationService {
  private final ReservationRepository reservations; private final RoomRepository rooms; private final ClosureRepository closures; private final AuthContext auth; private final AuditService audit; private final ReservationLifecycleService lifecycle;
  public ReservationService(ReservationRepository reservations, RoomRepository rooms, ClosureRepository closures, AuthContext auth, AuditService audit, ReservationLifecycleService lifecycle) { this.reservations=reservations; this.rooms=rooms; this.closures=closures; this.auth=auth; this.audit=audit; this.lifecycle=lifecycle; }
  @Transactional public Reservation create(ApiDtos.ReservationCreateRequest req) {
    User user=auth.approved(); var existing=reservations.findByApplicantAndRequestId(user,req.requestId()); if(existing.isPresent()) return existing.get();
    Room room=rooms.findByRoomNo(req.roomNo()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"ROOM_NOT_FOUND","会议室不存在")); int start=TimeSupport.minute(req.start()), end=TimeSupport.minute(req.end()); TimeSupport.checkDateTime(req.date(),start,end); if(room.getCapacity()!=null && req.attendeeCount()>room.getCapacity()) throw new ApiException(HttpStatus.BAD_REQUEST,"CAPACITY_EXCEEDED","预计人数超过会议室容量"); LocalDateTime from=TimeSupport.at(req.date(),start), to=TimeSupport.at(req.date(),end); if(!closures.findOverlapping(room,from,to).isEmpty()) throw new ApiException(HttpStatus.CONFLICT,"ROOM_CLOSED","该时段已停用"); lifecycle.reconcile(room,req.date()); if(!reservations.findOverlappingForUpdate(room,req.date(),start,end,List.of(Enums.ReservationStatus.PENDING,Enums.ReservationStatus.CONFIRMED)).isEmpty()) throw new ApiException(HttpStatus.CONFLICT,"TIME_CONFLICT","该时段已被占用");
    var r=new Reservation(); r.setId(UUID.randomUUID()); r.setRoom(room); r.setApplicant(user); r.setApplicantName(user.getName()); r.setApplicantPhone(req.phone()); r.setReservationDate(req.date()); r.setStartMinute(start); r.setEndMinute(end); r.setReason(req.reason()); r.setAttendeeCount(req.attendeeCount()); r.setNote(req.note()); r.setRequestId(req.requestId()); r.setStatus(user.getMemberType()==Enums.MemberType.STUDENT?Enums.ReservationStatus.PENDING:Enums.ReservationStatus.CONFIRMED); r.setCreatedAt(LocalDateTime.now()); r.setUpdatedAt(LocalDateTime.now()); var saved=reservations.save(r); audit.notify(user,saved.getStatus()==Enums.ReservationStatus.PENDING?Enums.NotificationType.IDENTITY:Enums.NotificationType.APPROVED,saved.getId(),saved.getStatus()==Enums.ReservationStatus.PENDING?"预约已提交，等待负责人确认":"预约成功"); return saved;
  }
  public List<Reservation> mine() { User u=auth.approved(); return reservations.findByApplicantOrderByReservationDateDescStartMinuteDesc(u); }
  public Reservation detail(UUID id) { User u=auth.approved(); Reservation r=reservations.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"RESERVATION_NOT_FOUND","预约不存在")); if(!r.getApplicant().getId().equals(u.getId()) && !u.isManager()) throw new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN","无权查看此预约"); lifecycle.reconcile(r.getRoom(),r.getReservationDate()); return r; }
  @Transactional public Reservation cancel(UUID id,String reason) { User u=auth.approved(); Reservation r=reservations.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"RESERVATION_NOT_FOUND","预约不存在")); if(!r.getApplicant().getId().equals(u.getId())) throw new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN","只能取消本人预约"); if(r.getStatus()!=Enums.ReservationStatus.PENDING && r.getStatus()!=Enums.ReservationStatus.CONFIRMED) return r; if(!TimeSupport.at(r.getReservationDate(),r.getStartMinute()).isAfter(LocalDateTime.now())) throw new ApiException(HttpStatus.CONFLICT,"ALREADY_STARTED","开始后不能自助取消"); r.setStatus(Enums.ReservationStatus.CANCELLED); r.setResultReason(reason); r.setUpdatedAt(LocalDateTime.now()); var saved=reservations.save(r); audit.notify(u,Enums.NotificationType.CANCELLED,r.getId(),"预约已取消"); return saved; }
}
