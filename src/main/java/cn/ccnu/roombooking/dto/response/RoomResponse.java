package cn.ccnu.roombooking.dto.response;

import cn.ccnu.roombooking.entity.Room;
import java.time.LocalTime;

public record RoomResponse(
    String roomNo,
    String location,
    Integer capacity,
    String equipment,
    String restrictionText,
    String infoStatus,
    LocalTime openTime,
    LocalTime closeTime) {

  public static RoomResponse of(Room room) {
    return new RoomResponse(room.getRoomNo(), room.getLocation(), room.getCapacity(),
        room.getEquipment(), room.getRestrictionText(), room.getInfoStatus(),
        room.getOpenTime(), room.getCloseTime());
  }
}
