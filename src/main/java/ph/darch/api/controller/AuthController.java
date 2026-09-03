package ph.darch.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ph.darch.api.dto.LoginRequest;
import ph.darch.api.dto.LoginResponse;
import ph.darch.api.exception.UnauthorizedException;
import ph.darch.api.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication for admin users")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Log in with admin credentials", description = "Returns a JWT bearer token")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        if (token == null) {
            throw new UnauthorizedException("Invalid username or password");
        }
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @Operation(summary = "Get the current authenticated admin's username")
    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }
        return Map.of("username", authentication.getName());
    }
}
