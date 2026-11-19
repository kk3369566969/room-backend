package cn.ccnu.roombooking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(
    @NotNull Boolean approved,
    @Size(max = 200) String reason) {
}
