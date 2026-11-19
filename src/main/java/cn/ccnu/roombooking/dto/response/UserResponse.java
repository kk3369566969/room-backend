package cn.ccnu.roombooking.dto.response;

import cn.ccnu.roombooking.entity.User;
import cn.ccnu.roombooking.enums.IdentityStatus;
import cn.ccnu.roombooking.enums.MemberType;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String externalId,
    String name,
    String employeeNo,
    MemberType memberType,
    String phone,
    IdentityStatus identityStatus,
    boolean manager,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static UserResponse of(User user) {
    if (user == null) {
      return null;
    }
    return new UserResponse(user.getId(), user.getExternalId(), user.getName(), user.getEmployeeNo(),
        user.getMemberType(), user.getPhone(), user.getIdentityStatus(), user.isManager(),
        user.getCreatedAt(), user.getUpdatedAt());
  }
}
