package cn.ccnu.roombooking.mapper;

import cn.ccnu.roombooking.entity.AuthSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;

public interface AuthSessionMapper extends BaseMapper<AuthSession> {

  default Optional<AuthSession> findByTokenHash(String tokenHash) {
    return Optional.ofNullable(
        selectOne(Wrappers.<AuthSession>lambdaQuery().eq(AuthSession::getTokenHash, tokenHash)));
  }
}
