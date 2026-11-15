package cn.ccnu.roombooking.common;

import cn.ccnu.roombooking.domain.Enums;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class ApiDtos {
  private ApiDtos() {}
  public record IdentitySubmitRequest(@NotBlank @Size(max=50) String name, @NotBlank @Size(max=30) String employeeNo, @NotNull Enums.MemberType memberType, @NotBlank @Pattern(regexp="1[3-9]\\d{9}") String phone) {}
  public record ReviewDecisionRequest(@NotNull Boolean approved, @Size(max=200) String reason) {}
  public record ReservationCreateRequest(@NotBlank String roomNo, @NotNull LocalDate date, @NotBlank String start, @NotBlank String end, @NotBlank @Size(max=200) String reason, @Positive int attendeeCount, @NotBlank @Pattern(regexp="1[3-9]\\d{9}") String phone, @Size(max=500) String note, @NotBlank @Size(max=64) String requestId) {}
  public record CancelRequest(@Size(max=200) String reason) {}
  public record ClosureRequest(@NotBlank String roomNo, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @NotBlank @Size(max=200) String reason, @NotNull Boolean confirm) {}
  public record RoomUpdateRequest(Integer capacity, String equipment, String restrictionText, String location) {}
  public record StatusResponse(String id, String status, String message) {}
  public record RoomResponse(String roomNo, String location, Integer capacity, String equipment, String restrictionText, String infoStatus, LocalTime openTime, LocalTime closeTime) {}
  public record ScheduleItem(String reservationId, String start, String end, String status, boolean own) {}
  public record ReservationResponse(String id, String roomNo, LocalDate date, String start, String end, String reason, int attendeeCount, String phone, String note, String status, String resultReason, String applicantName, String applicantPhone, LocalDateTime createdAt) {}
  public record NotificationResponse(String id, Enums.NotificationType type, UUID businessId, String content, LocalDateTime createdAt, LocalDateTime readAt) {}
}
