package cn.ccnu.roombooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ClosureRequest(
    @NotBlank String roomNo,
    @NotNull LocalDateTime startAt,
    @NotNull LocalDateTime endAt,
    @NotBlank @Size(max = 200) String reason,
    @NotNull Boolean confirm) {
}
