package com.resumeforge.resume.service;

import com.resumeforge.resume.auth.JwtUtil;
import com.resumeforge.resume.dto.AuthResponse;
import com.resumeforge.resume.dto.LoginRequest;
import com.resumeforge.resume.dto.RegisterRequest;
import com.resumeforge.resume.model.User;
import com.resumeforge.resume.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
    }

    // ── Email / Password Register ──────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists.");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(true);
        User saved = userRepository.save(user);

        log.info("[AUTH] Registered new user id={} email={}", saved.getId(), saved.getEmail());
        return toResponse(saved);
    }

    // ── Email / Password Login ────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid email or password."));

        if (user.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid email or password.");
        }

        log.info("[AUTH] Login successful userId={}", user.getId());
        return toResponse(user);
    }

    // ── /me ───────────────────────────────────────────────────────

    public AuthResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private AuthResponse toResponse(User user) {
        String token = jwtUtil.generate(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getName(),
                user.getEmail(), user.getPlan());
    }
}
