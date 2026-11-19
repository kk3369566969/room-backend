package cn.ccnu.roombooking.entity;

import cn.ccnu.roombooking.enums.ReservationStatus;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 会议室预约 */
@Getter
@Setter
@NoArgsConstructor
@TableName("reservation")
public class Reservation {

  @TableId(type = IdType.INPUT)
  private UUID id = UUID.randomUUID();

  private UUID roomId;
  private UUID applicantId;
  private String applicantName;
  private String applicantPhone;
  private LocalDate reservationDate;
  /** 当天 0 点起的分钟数 */
  private int startMinute;
  /** 当天 0 点起的分钟数 */
  private int endMinute;
  private String reason;
  private int attendeeCount;
  private String note;
  private ReservationStatus status;
  private String requestId;
  private UUID handledBy;
  private LocalDateTime handledAt;
  private String resultReason;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;
}
