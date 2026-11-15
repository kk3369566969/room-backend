package cn.ccnu.roombooking.web;
import cn.ccnu.roombooking.common.*;
import cn.ccnu.roombooking.domain.*;
import cn.ccnu.roombooking.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/admin")
public class AdminController {
  private final AdminService service; public AdminController(AdminService service) { this.service=service; }
  @GetMapping("/reviews") List<IdentityReview> reviews() { return service.reviews(); }
  @PostMapping("/reservations/{id}/approve") ApiDtos.ReservationResponse approve(@PathVariable UUID id) { return ReservationController.view(service.decide(id,true,null),true); }
  @PostMapping("/reservations/{id}/reject") ApiDtos.ReservationResponse reject(@PathVariable UUID id,@RequestBody ApiDtos.CancelRequest req) { return ReservationController.view(service.decide(id,false,req==null?null:req.reason()),true); }
  @GetMapping("/reservations") List<ApiDtos.ReservationResponse> ledger(@RequestParam(required=false) Enums.ReservationStatus status) { return service.ledger(status).stream().map(r->ReservationController.view(r,true)).toList(); }
  @PutMapping("/rooms/{roomNo}") ApiDtos.RoomResponse updateRoom(@PathVariable String roomNo,@RequestBody ApiDtos.RoomUpdateRequest req) { var r=service.updateRoom(roomNo,req); return new ApiDtos.RoomResponse(r.getRoomNo(),r.getLocation(),r.getCapacity(),r.getEquipment(),r.getRestrictionText(),r.getInfoStatus(),r.getOpenTime(),r.getCloseTime()); }
  @PostMapping("/closures/preview") List<ApiDtos.ReservationResponse> preview(@Valid @RequestBody ApiDtos.ClosureRequest req) { return service.closurePreview(req).stream().map(r->ReservationController.view(r,true)).toList(); }
  @PostMapping("/closures") ResponseEntity<String> createClosure(@Valid @RequestBody ApiDtos.ClosureRequest req) { return ResponseEntity.status(HttpStatus.CREATED).body(service.createClosure(req).getId().toString()); }
  @PostMapping("/closures/{id}/release") Closure release(@PathVariable UUID id) { return service.releaseClosure(id); }
  @GetMapping("/logs") List<OperationLog> logs() { return service.logs(); }
}
