package cn.ccnu.roombooking.dto.request;

public record RoomUpdateRequest(
    Integer capacity,
    String equipment,
    String restrictionText,
    String location) {
}
