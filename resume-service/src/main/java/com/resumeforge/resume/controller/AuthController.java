package com.resumeforge.resume.controller;

import com.resumeforge.resume.dto.AuthResponse;
import com.resumeforge.resume.dto.LoginRequest;
import com.resumeforge.resume.dto.RegisterRequest;
import com.resumeforge.resume.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:3000"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Create a new account — returns JWT + user info */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /** Sign in with email + password — returns JWT + user info */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Sign in with Google — expects { "idToken": "<google-credential>" }
     * The idToken is the credential from @react-oauth/google's CredentialResponse.
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleSignIn(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.googleSignIn(idToken));
    }

    /** Return current user info (and a fresh token) — requires valid JWT */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(authService.me(userId));
    }
}
