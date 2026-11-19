package cn.ccnu.roombooking.dto.request;

import jakarta.validation.constraints.Size;

public record CancelRequest(@Size(max = 200) String reason) {
}
