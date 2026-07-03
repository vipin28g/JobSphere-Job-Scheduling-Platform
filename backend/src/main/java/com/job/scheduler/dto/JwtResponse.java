package com.job.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    private UUID id;
    private String username;
    private String email;
    private String role;
}
