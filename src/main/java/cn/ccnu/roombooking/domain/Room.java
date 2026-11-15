package cn.ccnu.roombooking.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity @Table(name="room") @Getter @Setter @NoArgsConstructor
public class Room {
  @Id private UUID id;
  @Column(name="room_no", nullable=false, unique=true, length=16) private String roomNo;
  private String location;
  private Integer capacity;
  private String equipment;
  @Column(name="restriction_text") private String restrictionText;
  @Column(name="info_status", nullable=false) private String infoStatus;
  @Column(name="open_time", nullable=false) private LocalTime openTime;
  @Column(name="close_time", nullable=false) private LocalTime closeTime;
}
