package cn.ccnu.roombooking.dto.response;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    String externalId,
    boolean registered,
    String identityStatus,
    String memberType) {
}
