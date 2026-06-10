package com.enterprise.payment.controllers;

import com.enterprise.payment.dtos.AuthRequest;
import com.enterprise.payment.dtos.AuthResponse;
import com.enterprise.payment.dtos.RegisterRequest;
import com.enterprise.payment.entities.User;
import com.enterprise.payment.repositories.UserRepository;
import com.enterprise.payment.security.JwtTokenProvider;
import com.enterprise.payment.services.servicesImp.UserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * POST /api/auth/login
     *
     * WHAT IT DOES: Authenticates a user and returns a JWT token.
     *
     * WHAT HAPPENS (step by step):
     *   1. @Valid validates that username and password are not blank
     *   2. AuthenticationManager.authenticate() calls
     *      UserDetailsServiceImpl.loadUserByUsername()
     *      then compares the provided password with BCrypt hash in DB
     *   3. If credentials wrong → AuthenticationException → 401 returned
     *   4. If correct → Authentication object created with user + roles
     *   5. JwtTokenProvider.generateToken() creates a signed JWT
     *   6. Returns token + expiry time to client
     *
     * @RateLimiter: max 5 login attempts per second per instance.
     * Prevents brute-force password attacks.
     */
    @PostMapping("/login")
    @RateLimiter(name = "loginRateLimiter")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String token = tokenProvider.generateToken(user);

            log.info("User logged in: {}", request.getUsername());

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .username(user.getUsername())
                    .build());

        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .error("Invalid username or password")
                            .build());
        }
    }

    /**
     * POST /api/auth/register
     *
     * WHAT IT DOES: Creates a new user account.
     *
     * WHAT HAPPENS:
     *   1. @Valid validates email format, password strength constraints
     *   2. UserService.register() checks username/email not already taken
     *   3. Password is BCrypt-hashed before storage — never stored as plaintext
     *   4. User is assigned ROLE_USER by default
     *   5. Returns 201 Created — client must then call /login to get token
     *
     * Returning 201 without a token forces an explicit login step,
     * which allows for email verification workflows before granting access.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Registration successful. Please log in.");
    }
}

