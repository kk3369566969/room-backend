package cn.ccnu.roombooking.dto.response;

import cn.ccnu.roombooking.entity.Closure;
import cn.ccnu.roombooking.entity.Room;
import cn.ccnu.roombooking.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

public record ClosureResponse(
    UUID id,
    RoomResponse room,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String reason,
    UserResponse manager,
    boolean active,
    LocalDateTime createdAt) {

  public static ClosureResponse of(Closure closure, Room room, User manager) {
    return new ClosureResponse(closure.getId(), room == null ? null : RoomResponse.of(room),
        closure.getStartAt(), closure.getEndAt(), closure.getReason(), UserResponse.of(manager),
        closure.isActive(), closure.getCreatedAt());
  }
}
