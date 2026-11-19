package cn.ccnu.roombooking.entity;

import cn.ccnu.roombooking.enums.NotificationType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 站内通知 */
@Getter
@Setter
@NoArgsConstructor
@TableName("notification")
public class Notification {

  @TableId(type = IdType.INPUT)
  private UUID id = UUID.randomUUID();

  private UUID recipientId;
  private NotificationType type;
  private UUID businessId;
  private String content;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  private LocalDateTime readAt;
}
