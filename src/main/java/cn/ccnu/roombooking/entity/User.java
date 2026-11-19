package cn.ccnu.roombooking.entity;

import cn.ccnu.roombooking.enums.IdentityStatus;
import cn.ccnu.roombooking.enums.MemberType;
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

/** 用户（学生 / 教师 / 负责人） */
@Getter
@Setter
@NoArgsConstructor
@TableName("app_user")
public class User {

  @TableId(type = IdType.INPUT)
  private UUID id = UUID.randomUUID();

  private String externalId;
  private String name;
  private String employeeNo;
  private MemberType memberType;
  private String phone;
  private IdentityStatus identityStatus;
  private boolean manager;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  public boolean isApproved() {
    return identityStatus == IdentityStatus.APPROVED;
  }
}
