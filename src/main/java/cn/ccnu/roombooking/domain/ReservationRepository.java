package cn.ccnu.roombooking.domain;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
  Optional<Reservation> findByApplicantAndRequestId(User applicant, String requestId);
  List<Reservation> findByApplicantOrderByReservationDateDescStartMinuteDesc(User applicant);
  List<Reservation> findByApplicantAndReservationDateBetweenOrderByReservationDateAscStartMinuteAsc(User applicant, LocalDate from, LocalDate to);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Reservation r where r.room = :room and r.reservationDate = :date and r.status in :statuses and r.startMinute < :endMinute and r.endMinute > :startMinute")
  List<Reservation> findOverlappingForUpdate(@Param("room") Room room, @Param("date") LocalDate date, @Param("startMinute") int startMinute, @Param("endMinute") int endMinute, @Param("statuses") Collection<Enums.ReservationStatus> statuses);
  @Query("select r from Reservation r where r.room = :room and r.status in :statuses and r.reservationDate between :from and :to")
  List<Reservation> findActiveBetween(@Param("room") Room room, @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("statuses") Collection<Enums.ReservationStatus> statuses);
  List<Reservation> findByStatus(Enums.ReservationStatus status);
}
