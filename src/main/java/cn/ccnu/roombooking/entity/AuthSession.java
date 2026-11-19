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

/** 微信登录会话（只保存 token 摘要） */
@Getter
@Setter
@NoArgsConstructor
@TableName("auth_session")
public class AuthSession {

  @TableId(type = IdType.INPUT)
  private UUID id = UUID.randomUUID();

  private String tokenHash;
  private String externalId;
  private LocalDateTime expiresAt;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  private LocalDateTime revokedAt;

  public boolean isUsable() {
    return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
  }
}
