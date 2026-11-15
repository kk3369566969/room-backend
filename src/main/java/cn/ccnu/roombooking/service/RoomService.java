package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.common.*;
import cn.ccnu.roombooking.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class RoomService {
  private final RoomRepository rooms; private final ReservationRepository reservations; private final ClosureRepository closures; private final AuthContext auth; private final ReservationLifecycleService lifecycle;
  public RoomService(RoomRepository rooms, ReservationRepository reservations, ClosureRepository closures, AuthContext auth, ReservationLifecycleService lifecycle) { this.rooms=rooms; this.reservations=reservations; this.closures=closures; this.auth=auth; this.lifecycle=lifecycle; }
  public List<Room> list() { auth.approved(); return rooms.findAll(); }
  public Room room(String roomNo) { auth.approved(); return rooms.findByRoomNo(roomNo).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"ROOM_NOT_FOUND","会议室不存在")); }
  public List<ApiDtos.ScheduleItem> schedule(String roomNo, LocalDate date) {
    User me=auth.approved(); Room room=rooms.findByRoomNo(roomNo).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"ROOM_NOT_FOUND","会议室不存在")); lifecycle.reconcile(room, date);
    List<ApiDtos.ScheduleItem> out=new ArrayList<>(); for (Reservation r: reservations.findActiveBetween(room,date,date,List.of(Enums.ReservationStatus.PENDING,Enums.ReservationStatus.CONFIRMED))) out.add(new ApiDtos.ScheduleItem(r.getApplicant().getId().equals(me.getId())?r.getId().toString():null,TimeSupport.text(r.getStartMinute()),TimeSupport.text(r.getEndMinute()),r.getStatus().name(),r.getApplicant().getId().equals(me.getId()))); for (Closure c: closures.findOverlapping(room,TimeSupport.at(date,480),TimeSupport.at(date,1320))) out.add(new ApiDtos.ScheduleItem(null,c.getStartAt().toLocalTime().toString(),c.getEndAt().toLocalTime().toString(),"CLOSED",false)); return out;
  }
}
