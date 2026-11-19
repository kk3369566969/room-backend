package cn.ccnu.roombooking.mapper;

import cn.ccnu.roombooking.entity.Room;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;

public interface RoomMapper extends BaseMapper<Room> {

  default Optional<Room> findByRoomNo(String roomNo) {
    return Optional.ofNullable(
        selectOne(Wrappers.<Room>lambdaQuery().eq(Room::getRoomNo, roomNo)));
  }
}
