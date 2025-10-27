package com.venueconnect.auth;

import com.venueconnect.security.JwtService;
import com.venueconnect.user.Role;
import com.venueconnect.user.User;
import com.venueconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        // Create the user object
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // Hash the password
                .role(request.getRole() != null ? request.getRole() : Role.ROLE_USER) // Default to USER
                .build();

        // Save the user to the database
        userRepository.save(user);

        // Generate a JWT token
        var jwtToken = jwtService.generateToken(user);

        // Return the token in the response
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse login(LoginRequest request) {
        // This will authenticate the user or throw an exception if credentials are bad
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // If successful, find the user (we know they exist)
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(); // Should not happen if authentication passed

        // Generate a JWT token
        var jwtToken = jwtService.generateToken(user);

        // Return the token
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}