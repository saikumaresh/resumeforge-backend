package com.resumeforge.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeforge.resume.auth.JwtUtil;
import com.resumeforge.resume.dto.AuthResponse;
import com.resumeforge.resume.dto.LoginRequest;
import com.resumeforge.resume.dto.RegisterRequest;
import com.resumeforge.resume.model.User;
import com.resumeforge.resume.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String GOOGLE_TOKENINFO_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${google.client-id}")
    private String googleClientId;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       WebClient.Builder webClientBuilder,
                       ObjectMapper objectMapper) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
        this.webClient       = webClientBuilder.build();
        this.objectMapper    = objectMapper;
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

        // Detect Google-SSO-only accounts
        if (user.getPasswordHash() == null) {
            if (user.getGoogleId() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This account uses Google sign-in. Please click \"Continue with Google\" to log in.");
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid email or password.");
        }

        log.info("[AUTH] Login successful userId={}", user.getId());
        return toResponse(user);
    }

    // ── Google Sign-In ────────────────────────────────────────────

    /**
     * Verifies a Google ID token (from @react-oauth/google credential response)
     * with Google's tokeninfo endpoint, then creates or retrieves the local user.
     */
    @Transactional
    public AuthResponse googleSignIn(String idToken) {
        // Verify token with Google
        String raw;
        try {
            raw = webClient.get()
                    .uri(GOOGLE_TOKENINFO_URL + idToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            r -> r.bodyToMono(String.class).map(b ->
                                    new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                            "Invalid Google token: " + b)))
                    .bodyToMono(String.class)
                    .block();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AUTH] Google token verification failed", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Google sign-in failed. Please try again.");
        }

        JsonNode info;
        try {
            info = objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to parse Google response.");
        }

        // Validate audience matches our client ID
        String aud = info.path("aud").asText("");
        if (!googleClientId.equals("YOUR_GOOGLE_CLIENT_ID_HERE") && !aud.equals(googleClientId)) {
            log.warn("[AUTH] Google token audience mismatch: aud={}", aud);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Google token audience mismatch.");
        }

        String googleId   = info.path("sub").asText();
        String email      = info.path("email").asText("").toLowerCase().trim();
        String name       = info.path("name").asText("");
        String pictureUrl = info.path("picture").asText(null);

        if (email.isEmpty() || googleId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Incomplete Google profile.");
        }

        // Find existing user by googleId or email, or create new
        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setEmailVerified(true);
                    return u;
                });

        // Keep name/picture up-to-date from Google
        if (name != null && !name.isBlank()) user.setName(name);
        if (pictureUrl != null) user.setPictureUrl(pictureUrl);
        user.setGoogleId(googleId);
        if (!email.isBlank()) user.setEmail(email);
        user.setEmailVerified(true);

        User saved = userRepository.save(user);
        log.info("[AUTH] Google sign-in userId={} email={}", saved.getId(), saved.getEmail());
        return toResponse(saved);
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
                user.getEmail(), user.getPlan(), user.getPictureUrl());
    }
}
