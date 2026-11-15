package cn.ccnu.roombooking.web;

import cn.ccnu.roombooking.common.*;
import cn.ccnu.roombooking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService service) { this.service = service; }

  @PostMapping("/wechat-login")
  ResponseEntity<ApiDtos.LoginResponse> login(@Valid @RequestBody ApiDtos.WechatLoginRequest req) {
    return ResponseEntity.ok(service.login(req.code()));
  }

  @PostMapping("/logout")
  ResponseEntity<Void> logout() {
    service.logout();
    return ResponseEntity.noContent().build();
  }
}
