package vn.ueh.kltn.restaurant.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ueh.kltn.restaurant.dto.request.LoginRequest;
import vn.ueh.kltn.restaurant.dto.response.AccountResponse;
import vn.ueh.kltn.restaurant.dto.response.LoginResponse;
import vn.ueh.kltn.restaurant.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    public LoginResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public AccountResponse me() { return authService.me(); }
}
