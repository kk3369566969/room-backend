package cn.ccnu.roombooking.controller;

import cn.ccnu.roombooking.dto.request.CancelRequest;
import cn.ccnu.roombooking.dto.request.ReservationCreateRequest;
import cn.ccnu.roombooking.dto.response.ReservationResponse;
import cn.ccnu.roombooking.service.ReservationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationService reservationService;

  @PostMapping
  public ResponseEntity<ReservationResponse> create(
      @Valid @RequestBody ReservationCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request));
  }

  @GetMapping
  public List<ReservationResponse> mine() {
    return reservationService.mine();
  }

  @GetMapping("/{id}")
  public ReservationResponse detail(@PathVariable UUID id) {
    return reservationService.detail(id);
  }

  @PostMapping("/{id}/cancel")
  public ReservationResponse cancel(@PathVariable UUID id,
                                    @RequestBody(required = false) CancelRequest request) {
    return reservationService.cancel(id, request == null ? null : request.reason());
  }
}
