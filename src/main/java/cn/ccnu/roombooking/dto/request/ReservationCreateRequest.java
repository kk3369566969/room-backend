package cn.ccnu.roombooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ReservationCreateRequest(
    @NotBlank String roomNo,
    @NotNull LocalDate date,
    @NotBlank String start,
    @NotBlank String end,
    @NotBlank @Size(max = 200) String reason,
    @Positive int attendeeCount,
    @NotBlank @Pattern(regexp = "1[3-9]\\d{9}") String phone,
    @Size(max = 500) String note,
    @NotBlank @Size(max = 64) String requestId) {
}
