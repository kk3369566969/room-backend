package cn.ccnu.roombooking.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface NotificationRepository extends JpaRepository<Notification, UUID> { List<Notification> findByRecipientOrderByCreatedAtDesc(User user); }
