package com.job.scheduler.service;

import com.job.scheduler.dto.*;
import com.job.scheduler.entity.Organization;
import com.job.scheduler.entity.User;
import com.job.scheduler.entity.UserRole;
import com.job.scheduler.repository.OrganizationRepository;
import com.job.scheduler.repository.UserRepository;
import com.job.scheduler.security.JwtUtils;
import com.job.scheduler.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        String jwt = jwtUtils.generateJwtToken(userDetails.getUsername());
        var refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        return new JwtResponse(
                jwt,
                refreshToken.getToken(),
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                role);
    }

    @Transactional
    public void registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new IllegalArgumentException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        // Determine or create Organization
        String orgName = signUpRequest.getOrganizationName();
        if (orgName == null || orgName.trim().isEmpty()) {
            orgName = signUpRequest.getUsername() + "'s Org";
        }

        final String finalOrgName = orgName;
        Organization organization = organizationRepository.findByName(finalOrgName)
                .orElseGet(() -> {
                    Organization org = Organization.builder()
                            .name(finalOrgName)
                            .description("Default organization created during registration")
                            .createdAt(LocalDateTime.now())
                            .build();
                    return organizationRepository.save(org);
                });

        // Set role
        UserRole userRole = UserRole.VIEWER;
        if (signUpRequest.getRole() != null) {
            try {
                userRole = UserRole.valueOf(signUpRequest.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Error: Invalid role specify ADMIN, DEVELOPER, or VIEWER");
            }
        }

        // Create user
        User user = User.builder()
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .password(encoder.encode(signUpRequest.getPassword()))
                .role(userRole)
                .organization(organization)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public TokenRefreshResponse refreshAccessToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(com.job.scheduler.entity.RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateJwtToken(user.getUsername());
                    return new TokenRefreshResponse(token, requestRefreshToken);
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
