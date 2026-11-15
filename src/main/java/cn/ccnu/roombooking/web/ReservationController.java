package cn.ccnu.roombooking.web;
import cn.ccnu.roombooking.common.*;
import cn.ccnu.roombooking.domain.Reservation;
import cn.ccnu.roombooking.service.ReservationService;
import cn.ccnu.roombooking.service.TimeSupport;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/reservations")
public class ReservationController {
  private final ReservationService service; public ReservationController(ReservationService service) { this.service=service; }
  @PostMapping ResponseEntity<ApiDtos.ReservationResponse> create(@Valid @RequestBody ApiDtos.ReservationCreateRequest req) { return ResponseEntity.status(HttpStatus.CREATED).body(view(service.create(req),false)); }
  @GetMapping List<ApiDtos.ReservationResponse> mine() { return service.mine().stream().map(x->view(x,false)).toList(); }
  @GetMapping("/{id}") ApiDtos.ReservationResponse detail(@PathVariable UUID id) { return view(service.detail(id),true); }
  @PostMapping("/{id}/cancel") ApiDtos.ReservationResponse cancel(@PathVariable UUID id,@RequestBody(required=false) ApiDtos.CancelRequest req) { return view(service.cancel(id,req==null?null:req.reason()),true); }
  static ApiDtos.ReservationResponse view(Reservation r, boolean detail) { return new ApiDtos.ReservationResponse(r.getId().toString(),r.getRoom().getRoomNo(),r.getReservationDate(),TimeSupport.text(r.getStartMinute()),TimeSupport.text(r.getEndMinute()),r.getReason(),r.getAttendeeCount(),detail?r.getApplicantPhone():null, r.getNote(),r.getStatus().name(),r.getResultReason(),detail?r.getApplicantName():null,detail?r.getApplicantPhone():null,r.getCreatedAt()); }
}
