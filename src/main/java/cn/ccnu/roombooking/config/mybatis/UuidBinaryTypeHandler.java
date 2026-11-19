package cn.ccnu.roombooking.config.mybatis;

import java.nio.ByteBuffer;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * {@link UUID} 与 MySQL {@code BINARY(16)} 的互转。
 *
 * <p>表结构用 BINARY(16) 存主键/外键，比 CHAR(36) 更省空间；
 * 注册后实体里可以直接声明 UUID 字段，无需手工转换。
 */
@MappedTypes(UUID.class)
@MappedJdbcTypes(value = {JdbcType.BINARY, JdbcType.VARBINARY}, includeNullJdbcType = true)
public class UuidBinaryTypeHandler extends BaseTypeHandler<UUID> {

  private static final int UUID_BYTES = 16;

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setBytes(i, toBytes(parameter));
  }

  @Override
  public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return fromBytes(rs.getBytes(columnName));
  }

  @Override
  public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return fromBytes(rs.getBytes(columnIndex));
  }

  @Override
  public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return fromBytes(cs.getBytes(columnIndex));
  }

  private static byte[] toBytes(UUID uuid) {
    ByteBuffer buffer = ByteBuffer.allocate(UUID_BYTES);
    buffer.putLong(uuid.getMostSignificantBits());
    buffer.putLong(uuid.getLeastSignificantBits());
    return buffer.array();
  }

  private static UUID fromBytes(byte[] bytes) {
    if (bytes == null || bytes.length != UUID_BYTES) {
      return null;
    }
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    return new UUID(buffer.getLong(), buffer.getLong());
  }
}
