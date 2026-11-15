package cn.ccnu.roombooking.service;
import cn.ccnu.roombooking.common.ApiException;
import org.springframework.http.HttpStatus;
import java.time.*;
public final class TimeSupport {
  private TimeSupport() {}
  public static int minute(String value) { try { var t=LocalTime.parse(value); return t.getHour()*60+t.getMinute(); } catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_TIME","时间必须为 HH:mm"); } }
  public static String text(int minute) { return "%02d:%02d".formatted(minute/60, minute%60); }
  public static LocalDateTime at(LocalDate date, int minute) { return LocalDateTime.of(date, LocalTime.of(minute/60, minute%60)); }
  public static void checkDateTime(LocalDate date, int start, int end) { if (start < 480 || end > 1320 || start >= end || !date.equals(LocalDate.now())) { if (start < 480 || end > 1320 || start >= end) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_INTERVAL","预约必须在同日08:00-22:00且开始早于结束"); } if (!at(date,start).isAfter(LocalDateTime.now())) throw new ApiException(HttpStatus.BAD_REQUEST,"START_IN_PAST","开始时间必须晚于服务端当前时间"); }
}
