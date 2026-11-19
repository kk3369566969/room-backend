package cn.ccnu.roombooking.dto.response;

import cn.ccnu.roombooking.entity.IdentityReview;
import cn.ccnu.roombooking.entity.User;
import cn.ccnu.roombooking.enums.MemberType;
import cn.ccnu.roombooking.enums.ReviewStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record IdentityReviewResponse(
    UUID id,
    UserResponse user,
    String nameSnapshot,
    String employeeNoSnapshot,
    MemberType memberTypeSnapshot,
    String phoneSnapshot,
    ReviewStatus status,
    String reason,
    UserResponse reviewer,
    LocalDateTime submittedAt,
    LocalDateTime handledAt) {

  public static IdentityReviewResponse of(IdentityReview review, User user, User reviewer) {
    return new IdentityReviewResponse(review.getId(), UserResponse.of(user),
        review.getNameSnapshot(), review.getEmployeeNoSnapshot(), review.getMemberTypeSnapshot(),
        review.getPhoneSnapshot(), review.getStatus(), review.getReason(), UserResponse.of(reviewer),
        review.getSubmittedAt(), review.getHandledAt());
  }
}
