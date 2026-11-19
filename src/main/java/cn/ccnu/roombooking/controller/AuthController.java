package cn.ccnu.roombooking.controller;

import cn.ccnu.roombooking.dto.request.WechatLoginRequest;
import cn.ccnu.roombooking.dto.response.LoginResponse;
import cn.ccnu.roombooking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/wechat-login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody WechatLoginRequest request) {
    return ResponseEntity.ok(authService.login(request.code()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    authService.logout();
    return ResponseEntity.noContent().build();
  }
}
