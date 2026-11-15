package cn.ccnu.roombooking.domain;

public final class Enums {
  private Enums() {}
  public enum MemberType { STUDENT, TEACHER }
  public enum IdentityStatus { UNVERIFIED, PENDING, APPROVED, REJECTED, REVOKED }
  public enum ReviewStatus { PENDING, APPROVED, REJECTED }
  public enum ReservationStatus { PENDING, CONFIRMED, REJECTED, CANCELLED, EXPIRED, COMPLETED }
  public enum NotificationType { IDENTITY, APPROVED, REJECTED, CANCELLED, EXPIRED, CLOSURE }
  public enum ObjectType { USER, REVIEW, RESERVATION, ROOM, CLOSURE }
}
