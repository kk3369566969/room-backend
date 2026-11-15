package cn.ccnu.roombooking.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface IdentityReviewRepository extends JpaRepository<IdentityReview, UUID> {
  List<IdentityReview> findByStatusOrderBySubmittedAtAsc(Enums.ReviewStatus status);
}
