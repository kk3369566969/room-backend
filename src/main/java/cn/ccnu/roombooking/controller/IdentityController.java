package cn.ccnu.roombooking.controller;

import cn.ccnu.roombooking.dto.request.IdentitySubmitRequest;
import cn.ccnu.roombooking.dto.request.ReviewDecisionRequest;
import cn.ccnu.roombooking.dto.response.IdentityReviewResponse;
import cn.ccnu.roombooking.dto.response.StatusResponse;
import cn.ccnu.roombooking.dto.response.UserResponse;
import cn.ccnu.roombooking.security.AuthContext;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
@RequiredArgsConstructor
public class IdentityController {

  private final IdentityService identityService;
  private final AuthContext authContext;

  @PostMapping("/reviews")
  public ResponseEntity<StatusResponse> submit(@Valid @RequestBody IdentitySubmitRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(identityService.submit(request));
  }

  @GetMapping("/me")
  public UserResponse me() {
    return UserResponse.of(authContext.user());
  }

  @GetMapping("/reviews")
  public List<IdentityReviewResponse> pendingReviews() {
    return identityService.pendingReviews();
  }

  @PostMapping("/reviews/{id}/decision")
  public IdentityReviewResponse decide(@PathVariable UUID id,
                                       @Valid @RequestBody ReviewDecisionRequest request) {
    return identityService.decide(id, Boolean.TRUE.equals(request.approved()), request.reason());
  }
}
