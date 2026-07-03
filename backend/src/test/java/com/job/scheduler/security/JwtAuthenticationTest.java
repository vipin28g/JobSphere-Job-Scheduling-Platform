package com.job.scheduler.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtAuthenticationTest {

    private JwtUtils jwtUtils;

    // Use a test secret key that is 256-bit (32 bytes) HS256 compliant hex-encoded
    private final String testSecret = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";
    private final int testExpirationMs = 60000; // 60 seconds

    @BeforeEach
    public void setUp() {
        jwtUtils = new JwtUtils();
        // Manually inject values for testing since we aren't using @SpringBootTest here (faster unit test)
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", testExpirationMs);
    }

    @Test
    public void testGenerateAndParseTokenSuccess() {
        String username = "admin";
        String token = jwtUtils.generateJwtToken(username);

        assertNotNull(token);
        assertTrue(token.length() > 0);

        String parsedUsername = jwtUtils.getUserNameFromJwtToken(token);
        assertEquals(username, parsedUsername);
    }

    @Test
    public void testValidateValidToken() {
        String token = jwtUtils.generateJwtToken("developer");
        assertTrue(jwtUtils.validateJwtToken(token));
    }

    @Test
    public void testValidateInvalidToken() {
        String badToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.badtokenpayload.invalidsignature";
        assertFalse(jwtUtils.validateJwtToken(badToken));
    }

    @Test
    public void testValidateExpiredToken() {
        // Create jwtUtils instance with 0 expiration milliseconds
        JwtUtils expiredJwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(expiredJwtUtils, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(expiredJwtUtils, "jwtExpirationMs", -1000); // 1 second in the past

        String token = expiredJwtUtils.generateJwtToken("viewer");
        assertFalse(expiredJwtUtils.validateJwtToken(token));
    }
}
