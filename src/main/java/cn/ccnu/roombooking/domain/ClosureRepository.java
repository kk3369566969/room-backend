package cn.ccnu.roombooking.domain;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDateTime;
import java.util.*;
public interface ClosureRepository extends JpaRepository<Closure, UUID> {
  @Query("select c from Closure c where c.room = :room and c.active = true and c.startAt < :endAt and c.endAt > :startAt")
  List<Closure> findOverlapping(Room room, LocalDateTime startAt, LocalDateTime endAt);
}
