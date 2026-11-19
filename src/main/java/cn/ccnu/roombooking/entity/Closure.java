package cn.ccnu.roombooking.entity;

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

/** 会议室停用区间 */
@Getter
@Setter
@NoArgsConstructor
@TableName("closure")
public class Closure {

  @TableId(type = IdType.INPUT)
  private UUID id = UUID.randomUUID();

  private UUID roomId;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private String reason;
  private UUID managerId;
  private boolean active;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
}
