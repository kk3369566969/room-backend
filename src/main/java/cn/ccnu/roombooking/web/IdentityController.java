package cn.ccnu.roombooking.web;
import cn.ccnu.roombooking.common.*;
import cn.ccnu.roombooking.domain.*;
import cn.ccnu.roombooking.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/identity")
public class IdentityController {
  private final IdentityService service; private final AuthContext auth;
  public IdentityController(IdentityService service, AuthContext auth) { this.service=service; this.auth=auth; }
  @PostMapping("/reviews") ResponseEntity<ApiDtos.StatusResponse> submit(@Valid @RequestBody ApiDtos.IdentitySubmitRequest req) { var r=service.submit(req); return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDtos.StatusResponse(r.getId().toString(),r.getStatus().name(),"身份资料已提交")); }
  @GetMapping("/me") User me() { return auth.user(); }
  @GetMapping("/reviews") List<IdentityReview> pending() { return service.pending(); }
  @PostMapping("/reviews/{id}/decision") IdentityReview decision(@PathVariable UUID id,@Valid @RequestBody ApiDtos.ReviewDecisionRequest req) { return service.decide(id,Boolean.TRUE.equals(req.approved()),req.reason()); }
}
