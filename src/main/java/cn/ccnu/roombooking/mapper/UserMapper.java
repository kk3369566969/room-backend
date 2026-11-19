package cn.ccnu.roombooking.mapper;

import cn.ccnu.roombooking.entity.User;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Optional;

public interface UserMapper extends BaseMapper<User> {

  default Optional<User> findByExternalId(String externalId) {
    return Optional.ofNullable(
        selectOne(Wrappers.<User>lambdaQuery().eq(User::getExternalId, externalId)));
  }

  default Optional<User> findByEmployeeNo(String employeeNo) {
    return Optional.ofNullable(
        selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmployeeNo, employeeNo)));
  }
}
