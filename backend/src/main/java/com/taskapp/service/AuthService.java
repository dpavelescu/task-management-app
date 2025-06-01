package com.taskapp.service;

import com.taskapp.dto.AuthResponse;
import com.taskapp.dto.LoginRequest;
import com.taskapp.dto.RegisterRequest;
import com.taskapp.entity.User;
import com.taskapp.exception.AuthenticationException;
import com.taskapp.exception.UserNotFoundException;
import com.taskapp.repository.UserRepository;
import com.taskapp.security.JwtTokenProvider;
import com.taskapp.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    
    // Business event logger for audit trail
    private static final org.slf4j.Logger businessLog = org.slf4j.LoggerFactory.getLogger("business-events");@Transactional
    public AuthResponse register(RegisterRequest request) {
        log.debug("Attempting to register user: {}", request.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username '{}' already exists", request.getUsername());
            throw new IllegalArgumentException("Username is already taken");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email '{}' already exists", request.getEmail());
            throw new IllegalArgumentException("Email is already in use");
        }

        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));            User savedUser = userRepository.save(user);
            log.info("User '{}' registered successfully", savedUser.getUsername());
            businessLog.info("USER_REGISTERED: username={}, email={}, id={}", 
                savedUser.getUsername(), savedUser.getEmail(), savedUser.getId());

            var userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String token = jwtTokenProvider.generateToken(userDetails);

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                savedUser.getId(), 
                savedUser.getUsername(), 
                savedUser.getEmail()
            );
            
            return new AuthResponse(token, userInfo);
        } catch (Exception e) {
            log.error("Registration failed for user '{}': {}", request.getUsername(), e.getMessage());
            throw new AuthenticationException("Registration failed: " + e.getMessage());
        }
    }    public AuthResponse login(LoginRequest request) {
        log.debug("Attempting login for user: {}", request.getUsername());
        
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("username", request.getUsername()));            var userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            String token = jwtTokenProvider.generateToken(userDetails);
            
            log.debug("User '{}' logged in successfully", request.getUsername());
            businessLog.info("USER_LOGIN: username={}", request.getUsername());
            
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(), 
                user.getUsername(), 
                user.getEmail()
            );
            return new AuthResponse(token, userInfo);        } catch (BadCredentialsException e) {
            log.warn("Login failed for user '{}': Invalid credentials", request.getUsername());
            businessLog.warn("LOGIN_FAILED: username={}, reason=invalid_credentials", request.getUsername());
            throw new AuthenticationException("Invalid username or password");
        } catch (Exception e) {
            log.error("Login failed for user '{}': {}", request.getUsername(), e.getMessage());
            throw new AuthenticationException("Login failed: " + e.getMessage());
        }
    }
}
