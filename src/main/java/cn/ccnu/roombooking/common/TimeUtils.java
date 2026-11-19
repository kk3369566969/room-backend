package cn.ccnu.roombooking.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约时间工具：数据库用「分钟数」存储时段，对外用 "HH:mm" 文本。
 */
public final class TimeUtils {

  /** 开放时段 08:00 */
  public static final int OPEN_MINUTE = 8 * 60;
  /** 开放时段 22:00 */
  public static final int CLOSE_MINUTE = 22 * 60;

  private TimeUtils() {}

  public static int toMinute(String text) {
    try {
      LocalTime time = LocalTime.parse(text);
      return time.getHour() * 60 + time.getMinute();
    } catch (RuntimeException e) {
      throw ApiException.badRequest("INVALID_TIME", "时间必须为 HH:mm");
    }
  }

  public static String toText(int minute) {
    return "%02d:%02d".formatted(minute / 60, minute % 60);
  }

  public static LocalDateTime at(LocalDate date, int minute) {
    return LocalDateTime.of(date, LocalTime.of(minute / 60, minute % 60));
  }

  /**
   * 校验预约时段：必须落在开放时段内、开始早于结束、且开始时间晚于当前时间。
   */
  public static void validateBookable(LocalDate date, int startMinute, int endMinute) {
    boolean outOfOpenHours = startMinute < OPEN_MINUTE
        || endMinute > CLOSE_MINUTE
        || startMinute >= endMinute;
    if (outOfOpenHours) {
      throw ApiException.badRequest("INVALID_INTERVAL", "预约必须在同日08:00-22:00且开始早于结束");
    }
    if (!at(date, startMinute).isAfter(LocalDateTime.now())) {
      throw ApiException.badRequest("START_IN_PAST", "开始时间必须晚于服务端当前时间");
    }
  }
}
