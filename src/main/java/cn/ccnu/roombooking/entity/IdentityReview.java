package cn.ccnu.roombooking.entity;

import cn.ccnu.roombooking.enums.MemberType;
import cn.ccnu.roombooking.enums.ReviewStatus;
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

/** 身份审核记录（保存提交时的资料快照） */
@Getter
@Setter
@NoArgsConstructor
@TableName("identity_review")
public class IdentityReview {

  @TableId(type = IdType.INPUT)
  private UUID id = UUID.randomUUID();

  private UUID userId;
  private String nameSnapshot;
  private String employeeNoSnapshot;
  private MemberType memberTypeSnapshot;
  private String phoneSnapshot;
  private ReviewStatus status;
  private String reason;
  private UUID reviewerId;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime submittedAt;

  private LocalDateTime handledAt;
}
