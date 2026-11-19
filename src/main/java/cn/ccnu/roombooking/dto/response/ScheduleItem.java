package cn.ccnu.roombooking.dto.response;

/** 某天某会议室的时段条目；own=false 时 reservationId 为空，避免泄露他人预约号。 */
public record ScheduleItem(String reservationId, String start, String end, String status, boolean own) {
}
