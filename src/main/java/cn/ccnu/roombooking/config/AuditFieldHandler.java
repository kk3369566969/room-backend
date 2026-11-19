package cn.ccnu.roombooking.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 自动填充审计字段：插入时补 createdAt / updatedAt，更新时刷新 updatedAt。
 * 只对实体上标注了 {@code @TableField(fill = ...)} 的字段生效。
 */
@Component
public class AuditFieldHandler implements MetaObjectHandler {

  @Override
  public void insertFill(MetaObject metaObject) {
    LocalDateTime now = LocalDateTime.now();
    strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
    strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    strictInsertFill(metaObject, "submittedAt", LocalDateTime.class, now);
  }

  @Override
  public void updateFill(MetaObject metaObject) {
    strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
  }
}
