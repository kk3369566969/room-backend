package cn.ccnu.roombooking.dto.request;

import cn.ccnu.roombooking.enums.MemberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IdentitySubmitRequest(
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Size(max = 30) String employeeNo,
    @NotNull MemberType memberType,
    @NotBlank @Pattern(regexp = "1[3-9]\\d{9}") String phone) {
}
