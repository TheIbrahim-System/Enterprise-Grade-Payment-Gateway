package com.enterprise.payment.security;


import com.enterprise.payment.entities.User;
import com.enterprise.payment.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * WHAT IT DOES: Loads a user from the database by username.
     * Called automatically by Spring Security during authentication.
     * <p>
     * WHAT HAPPENS (two scenarios):
     * <p>
     * Scenario A — Login (/api/auth/login):
     * 1. AuthController calls AuthenticationManager.authenticate()
     * 2. Spring calls this method to get the UserDetails
     * 3. Spring compares the provided password with user.getPassword()
     * using BCryptPasswordEncoder.matches()
     * 4. If match → authentication succeeds
     * 5. JwtTokenProvider generates token with roles from UserDetails
     * <p>
     * Scenario B — JWT Request (any protected endpoint):
     * 1. JwtAuthFilter extracts username from JWT payload
     * 2. Calls this method to load current user state from DB
     * 3. Checks account is still enabled, not locked
     * 4. Sets UserDetails in SecurityContext for @PreAuthorize checks
     *
     * @Transactional(readOnly=true): ensures roles (EAGER fetch) are loaded
     * within the transaction. Without this, LazyInitializationException
     * can occur when accessing user.getRoles() outside a transaction.
     * <p>
     * UsernameNotFoundException: Spring Security catches this internally
     * and converts it to a 401 response — username is never leaked.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return
        userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException(
                            "User not found: " + username);
                });


    }
}
