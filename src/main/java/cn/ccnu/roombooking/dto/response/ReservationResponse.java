package cn.ccnu.roombooking.dto.response;

import cn.ccnu.roombooking.common.TimeUtils;
import cn.ccnu.roombooking.entity.Reservation;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
    String id,
    String roomNo,
    LocalDate date,
    String start,
    String end,
    String reason,
    int attendeeCount,
    String phone,
    String note,
    String status,
    String resultReason,
    String applicantName,
    String applicantPhone,
    LocalDateTime createdAt) {

  /**
   * @param detail 是否返回申请人姓名等明细字段（列表接口不返回）
   */
  public static ReservationResponse of(Reservation reservation, String roomNo, boolean detail) {
    return new ReservationResponse(
        reservation.getId().toString(),
        roomNo,
        reservation.getReservationDate(),
        TimeUtils.toText(reservation.getStartMinute()),
        TimeUtils.toText(reservation.getEndMinute()),
        reservation.getReason(),
        reservation.getAttendeeCount(),
        reservation.getApplicantPhone(),
        reservation.getNote(),
        reservation.getStatus().name(),
        reservation.getResultReason(),
        detail ? reservation.getApplicantName() : null,
        detail ? reservation.getApplicantPhone() : null,
        reservation.getCreatedAt());
  }
}
