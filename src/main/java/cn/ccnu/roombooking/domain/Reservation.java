package cn.ccnu.roombooking.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="reservation", indexes=@Index(name="idx_res_overlap", columnList="room_id,reservation_date,start_minute,end_minute,status")) @Getter @Setter @NoArgsConstructor
public class Reservation {
  @Id private UUID id;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="room_id", nullable=false) private Room room;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="applicant_id", nullable=false) private User applicant;
  @Column(name="applicant_name", nullable=false) private String applicantName;
  @Column(name="applicant_phone", nullable=false) private String applicantPhone;
  @Column(name="reservation_date", nullable=false) private LocalDate reservationDate;
  @Column(name="start_minute", nullable=false) private int startMinute;
  @Column(name="end_minute", nullable=false) private int endMinute;
  @Column(nullable=false) private String reason;
  @Column(name="attendee_count", nullable=false) private int attendeeCount;
  private String note;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Enums.ReservationStatus status;
  @Column(name="request_id", nullable=false, length=64) private String requestId;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="handled_by") private User handledBy;
  private LocalDateTime handledAt;
  @Column(name="result_reason") private String resultReason;
  @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
  @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
}
