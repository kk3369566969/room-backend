package cn.ccnu.roombooking.entity;

import cn.ccnu.roombooking.enums.ObjectType;
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

/** 管理端操作日志 */
@Getter
@Setter
@NoArgsConstructor
@TableName("operation_log")
public class OperationLog {

  @TableId(type = IdType.INPUT)
  private UUID id = UUID.randomUUID();

  private UUID operatorId;
  private ObjectType objectType;
  private UUID objectId;
  private String action;
  private String result;
  private String summary;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
}
