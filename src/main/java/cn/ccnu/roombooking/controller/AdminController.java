package cn.ccnu.roombooking.controller;

import cn.ccnu.roombooking.dto.request.CancelRequest;
import cn.ccnu.roombooking.dto.request.ClosureRequest;
import cn.ccnu.roombooking.dto.request.RoomUpdateRequest;
import cn.ccnu.roombooking.dto.response.ClosureResponse;
import cn.ccnu.roombooking.dto.response.IdentityReviewResponse;
import cn.ccnu.roombooking.dto.response.OperationLogResponse;
import cn.ccnu.roombooking.dto.response.ReservationResponse;
import cn.ccnu.roombooking.dto.response.RoomResponse;
import cn.ccnu.roombooking.enums.ReservationStatus;
import cn.ccnu.roombooking.service.AdminService;
import cn.ccnu.roombooking.service.IdentityService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

  private final AdminService adminService;
  private final IdentityService identityService;

  /** 待审核的身份申请列表（身份审核属于身份域，直接走身份服务） */
  @GetMapping("/reviews")
  public List<IdentityReviewResponse> reviews() {
    return identityService.pendingReviews();
  }

  @PostMapping("/reservations/{id}/approve")
  public ReservationResponse approve(@PathVariable UUID id) {
    return adminService.decide(id, true, null);
  }

  @PostMapping("/reservations/{id}/reject")
  public ReservationResponse reject(@PathVariable UUID id,
                                    @RequestBody(required = false) CancelRequest request) {
    return adminService.decide(id, false, request == null ? null : request.reason());
  }

  @GetMapping("/reservations")
  public List<ReservationResponse> ledger(
      @RequestParam(required = false) ReservationStatus status) {
    return adminService.ledger(status);
  }

  @PutMapping("/rooms/{roomNo}")
  public RoomResponse updateRoom(@PathVariable String roomNo,
                                 @RequestBody RoomUpdateRequest request) {
    return adminService.updateRoom(roomNo, request);
  }

  @PostMapping("/closures/preview")
  public List<ReservationResponse> previewClosure(@Valid @RequestBody ClosureRequest request) {
    return adminService.closurePreview(request);
  }

  @PostMapping("/closures")
  public ResponseEntity<String> createClosure(@Valid @RequestBody ClosureRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(adminService.createClosure(request).toString());
  }

  @PostMapping("/closures/{id}/release")
  public ClosureResponse releaseClosure(@PathVariable UUID id) {
    return adminService.releaseClosure(id);
  }

  @GetMapping("/logs")
  public List<OperationLogResponse> logs() {
    return adminService.logs();
  }
}
