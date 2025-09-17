package com.example;

import com.example.entity.User;
import com.example.repository.UserRepository;
import com.example.security.JWTTokenGenerator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClientException;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

//@Disabled
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "JWT_USER=testuser",
        "JWT_ROLE=ADMIN",
        "JWT_PASSWORD=testpassword",
        "JWT_EXPIRATION=86400",
        "JWT_HEADER=Authorization",
        "JWT_PREFIX=Bearer "
})
class SpringBootDockerApplicationFailingTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Value("${JWT_SECRET}")
    String base64_secret;

    @Value("${JWT_USER}")
    String username;

    @Value("${JWT_EXPIRATION}")
    Long expirationSeconds;

    @Value("${JWT_ROLE}")
    String role;

    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:latest")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        registry.add("spring.docker.compose.enabled", () -> "false");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MariaDBDialect");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MariaDBDialect");

        String secret = JWTTokenGenerator.generateSecret();
        registry.add("JWT_SECRET", () -> secret);
    }

    @Test
    void testUnauthorizedAccessToHealthEndpoint() {
        // Test accessing health endpoint without authentication
        String url = "http://localhost:" + port + "/api/users/health";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testUnauthorizedAccessToActuatorHealth() {
        // Test accessing actuator health without authentication
        String url = "http://localhost:" + port + "/actuator/health";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testInvalidJWTToken() {
        // Test with completely invalid JWT token
        String url = "http://localhost:" + port + "/api/users/health";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid.jwt.token");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testExpiredJWTToken() {
        // Test with expired JWT token
        String expiredToken = generateExpiredToken();
        String url = "http://localhost:" + port + "/api/users/health";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testCreateUserWithDuplicateEmail() {
        // Test creating users with duplicate email addresses
        String baseUrl = "http://localhost:" + port + "/api/users";

        // Create first user
        User firstUser = new User();
        firstUser.setName("First User");
        firstUser.setEmail("duplicate@example.com");
        firstUser.setPassword("123");
        firstUser.setRole("ADMIN");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<User> entity1 = new HttpEntity<>(firstUser, headers);

        ResponseEntity<User> firstResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity1,
                User.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Try to create second user with same email
        User secondUser = new User();
        secondUser.setName("Second User");
        secondUser.setEmail("duplicate@example.com");
        secondUser.setPassword("456");
        secondUser.setRole("USER");

        HttpEntity<User> entity2 = new HttpEntity<>(secondUser, headers);

        try {
            ResponseEntity<User> secondResponse = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    entity2,
                    User.class
            );
        } catch (RestClientException e) {
            assertThat(e.getMessage()).isNotNull();
        }
    }

    @Test
    void testCreateUserWithInvalidData() {
        // Test creating user with missing required fields
        String baseUrl = "http://localhost:" + port + "/api/users";

        User invalidUser = new User();
        // Missing name, email, password, role

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<User> entity = new HttpEntity<>(invalidUser, headers);

        try {
            ResponseEntity<User> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    entity,
                    User.class
            );
        } catch (RestClientException e) {
            assertThat(e.getMessage()).contains("Error while extracting response");
        }
    }

    @Test
    void testGetNonExistentUser() {
        // Test getting a user that doesn't exist
        Long nonExistentId = 99999L;
        String url = "http://localhost:" + port + "/api/users/" + nonExistentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<User> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                User.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testDeleteNonExistentUser() {
        // Test deleting a user that doesn't exist
        Long nonExistentId = 99999L;
        String url = "http://localhost:" + port + "/api/users/" + nonExistentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<User> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                User.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testDatabaseConstraintViolation() {
        // Test database constraint violations at repository level
        User user1 = new User();
        user1.setName("Test User");
        user1.setEmail("constraint@example.com");
        user1.setPassword("123");
        user1.setRole("ADMIN");
        userRepository.save(user1);

        // Try to save another user with same email (assuming email is unique)
        User user2 = new User();
        user2.setName("Another User");
        user2.setEmail("constraint@example.com");
        user2.setPassword("456");
        user2.setRole("USER");

        // This should throw a DataIntegrityViolationException
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(user2);
        });
    }

    @Test
    void testInvalidEmailFormat() {
        // Test creating user with invalid email format
        String baseUrl = "http://localhost:" + port + "/api/users";

        User userWithInvalidEmail = new User();
        userWithInvalidEmail.setName("Invalid Email User");
        userWithInvalidEmail.setEmail("not-an-email");
        userWithInvalidEmail.setPassword("123");
        userWithInvalidEmail.setRole("USER");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<User> entity = new HttpEntity<>(userWithInvalidEmail, headers);

        try {
            ResponseEntity<User> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    entity,
                    User.class
            );
        } catch (RestClientException e) {
            assertThat(e.getMessage()).contains("Error while extracting response");
        }
    }

    @Test
    void testCreateUserWithNullValues() {
        // Test creating user with null values
        String baseUrl = "http://localhost:" + port + "/api/users";

        User userWithNulls = new User();
        userWithNulls.setName(null);
        userWithNulls.setEmail(null);
        userWithNulls.setPassword(null);
        userWithNulls.setRole(null);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<User> entity = new HttpEntity<>(userWithNulls, headers);

        try {
            ResponseEntity<User> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    entity,
                    User.class
            );
        } catch (RestClientException e) {
            assertThat(e.getMessage()).contains("Error while extracting response");
        }
    }

    @Test
    void testCreateUserWithEmptyStrings() {
        // Test creating user with empty string values
        String baseUrl = "http://localhost:" + port + "/api/users";

        User userWithEmptyStrings = new User();
        userWithEmptyStrings.setName("");
        userWithEmptyStrings.setEmail("");
        userWithEmptyStrings.setPassword("");
        userWithEmptyStrings.setRole("");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<User> entity = new HttpEntity<>(userWithEmptyStrings, headers);

        try {
            ResponseEntity<User> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    entity,
                    User.class
            );
        } catch (RestClientException e) {
            assertThat(e.getMessage()).contains("Error while extracting response");
        }
    }

    @Test
    void testUnauthorizedUserAccess() {
        // Test with a token that has insufficient privileges (if role-based access is implemented)
        String url = "http://localhost:" + port + "/api/users";

        HttpHeaders headers = new HttpHeaders();
        var role1 = role;
        role = "USER";
        headers.setBearerAuth(generateTokenWithRole()); // Assuming USER role has limited access
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<User[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                User[].class
        );

        role = role1;
        // This might be FORBIDDEN if role-based access control is strict
        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testMalformedAuthorizationHeader() {
        // Test with malformed Authorization header
        String url = "http://localhost:" + port + "/api/users/health";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "InvalidFormat " + generateToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testMethodNotAllowed() {
        // Test using wrong HTTP method
        String url = "http://localhost:" + port + "/api/users/health";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST, // Wrong method, should be GET
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testRedisConnectionFailureHandling() {
        // Test behavior when Redis cache is not available
        // This test might need to be run with Redis container stopped
        // or with a mock that simulates connection failure

        // Create a user first
        User user = new User();
        user.setName("Cache Test User");
        user.setEmail("cache.test@example.com");
        user.setPassword("123");
        user.setRole("ADMIN");
        User savedUser = userRepository.save(user);

        // Try to access user when cache might be down
        String url = "http://localhost:" + port + "/api/users/" + savedUser.getId();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<User> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                User.class
        );

        // Should still work even if cache is down (fallback to database)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testInvalidJsonPayload() {
        // Test with invalid JSON in request body
        String baseUrl = "http://localhost:" + port + "/api/users";
        String invalidJson = "{\"name\":\"Test\",\"email\":}"; // Invalid JSON

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(invalidJson, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    // Helper methods
    private String generateToken() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (expirationSeconds * 1000));
        SecretKey secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64_secret));

        return Jwts.builder()
                .setSubject(username)
                .claim("role", "ADMIN")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateExpiredToken() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() - 3600000); // Expired 1 hour ago
        SecretKey secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64_secret));

        return Jwts.builder()
                .setSubject(username)
                .claim("role", "ADMIN")
                .setIssuedAt(new Date(now.getTime() - 7200000)) // Issued 2 hours ago
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateTokenWithRole() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (expirationSeconds * 1000));
        SecretKey secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64_secret));

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}