package com.example;

import com.example.entity.User;
import com.example.repository.UserRepository;
import com.example.security.JWTTokenGenerator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "JWT_USER=testuser",
        "JWT_PASSWORD=testpassword",
        "JWT_EXPIRATION=86400",
        "JWT_HEADER=Authorization",
        "JWT_PREFIX=Bearer "
})
@Disabled
class SpringBootDockerApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${JWT_SECRET}")
    String base64_secret;

    @Value("${JWT_USER}")
    String username;

    @Value("${JWT_EXPIRATION}")
    Long expirationSeconds;

    // Test containers for isolated testing
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
        // Override database properties for testing
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");

        // Override Redis properties for testing
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        // Disable Docker Compose for tests
        registry.add("spring.docker.compose.enabled", () -> "false");

        // MariaDB specific JPA properties
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MariaDBDialect");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MariaDBDialect");

        String secret = JWTTokenGenerator.generateSecret();
        registry.add("JWT_SECRET", ( )-> secret);
    }

    @Test
    void contextLoads() {
        // Verify that the Spring Boot application context loads successfully
        assertThat(userRepository).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    void testMariaDBConnection() {
        // Test that we can connect to the MariaDB database
        assertThat(mariadb.isRunning()).isTrue();
        assertThat(mariadb.isCreated()).isTrue();

        // Verify MariaDB specific details
        assertThat(mariadb.getJdbcUrl()).contains("mariadb");
        assertThat(mariadb.getDatabaseName()).isEqualTo("testdb");

        // Test basic database operation
        User user = new User();
        user.setName("MariaDB Test User");
        user.setEmail("mariadb@example.com");
        user.setPassword("123");
        user.setRole("admin");
        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("MariaDB Test User");
        assertThat(savedUser.getEmail()).isEqualTo("mariadb@example.com");

        // Verify user can be found
        assertThat(userRepository.findById(savedUser.getId())).isPresent();

        // Test MariaDB specific functionality
        assertThat(userRepository.existsByEmail("mariadb@example.com")).isTrue();
        assertThat(userRepository.findByEmail("mariadb@example.com")).isPresent();
    }

    @Test
    void testRedisConnection() {
        // Test that we can connect to Redis
        assertThat(redis.isRunning()).isTrue();

        // Test basic Redis operation
        String key = "test:mariadb:key";
        String value = "mariadb-test-value";

        redisTemplate.opsForValue().set(key, value);
        Object retrievedValue = redisTemplate.opsForValue().get(key);

        assertThat(retrievedValue).isEqualTo(value);

        // Clean up
        redisTemplate.delete(key);
    }

    @Test
    void testHealthEndpoint() {

        // Test the api health endpoint

        String url = "http://localhost:" + port + "/api/users/health";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Application is running");

    }

    @Test
    void testActuatorHealthEndpoint() {
        // Test the actuator health endpoint

        String url = "http://localhost:" + port + "/actuator/health";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void testUserCrudOperationsWithMariaDB() {
        // Test complete CRUD operations via REST API with MariaDB
        String baseUrl = "http://localhost:" + port + "/api/users";

        // CREATE - Post a new user
        User newUser = new User();
        newUser.setName("Maria DB User");
        newUser.setEmail("maria.db@example.com");
        newUser.setPassword("123");
        newUser.setRole("admin");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<User> entity = new HttpEntity<>(newUser, headers);

        ResponseEntity<User> createResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity,
                User.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo("Maria DB User");
        assertThat(createResponse.getBody().getId()).isNotNull();

        Long userId = createResponse.getBody().getId();

        // READ - Get the created user

        HttpEntity<User> entity1 = new HttpEntity<>(headers);
        ResponseEntity<User> getResponse = restTemplate.exchange(
                baseUrl + "/" + userId,
                HttpMethod.GET,
                entity1,
                User.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getName()).isEqualTo("Maria DB User");

        // DELETE - Delete the user
        //restTemplate.delete(baseUrl + "/" + userId);
        restTemplate.exchange(
                baseUrl + "/" + userId,
                HttpMethod.DELETE,
                entity1,
                User.class
        );

        // Verify deletion

        ResponseEntity<User> deletedResponse = restTemplate.exchange(
                baseUrl + "/" + userId,
                HttpMethod.GET,
                entity1,
                User.class
        );

        assertThat(deletedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUserCachingWithMariaDB() {
        // Test that Redis caching works correctly with MariaDB backend
        User user = new User();
        user.setName("Cached MariaDB User");
        user.setEmail("cached.mariadb@example.com");
        user.setPassword("123");
        user.setRole("admin");
        User savedUser = userRepository.save(user);

        // First request - should cache the user
        String url = "http://localhost:" + port + "/api/users/" + savedUser.getId();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<User> firstResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                User.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstResponse.getBody()).isNotNull();

        // Check if user is in Redis cache
        Object cachedUser = redisTemplate.opsForValue().get("user:" + savedUser.getId());
        assertThat(cachedUser).isNotNull();

        // Second request - should come from cache
        ResponseEntity<User> secondResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                User.class
        );
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(secondResponse.getBody());
        assertThat(secondResponse.getBody().getName()).isEqualTo("Cached MariaDB User");
    }

    @Test
    //@WithMockUser(username = "admin", roles = {"ADMIN"})
    //@AutoConfigureMockMvc(addFilters = false)
    void testGetAllUsersWithMariaDB() {
        // Clean up existing users first
        userRepository.deleteAll();

        // Create some test users with MariaDB specific data
        var newUser1 = new User();
        newUser1.setName("MariaDB User one");
        newUser1.setEmail("maria1@example.com");
        newUser1.setPassword("123");
        newUser1.setRole("admin");
        var newUser2 = new User();
        newUser2.setName("MariaDB User two");
        newUser2.setEmail("maria2@example.com");
        newUser2.setPassword("123");
        newUser2.setRole("admin");
        var newUser3 = new User();
        newUser3.setName("MariaDB User three");
        newUser3.setEmail("maria3@example.com");
        newUser3.setPassword("123");
        newUser3.setRole("admin");
        userRepository.save(newUser1);
        userRepository.save(newUser2);
        userRepository.save(newUser3);

        // Get all users via REST API
        String url = "http://localhost:" + port + "/api/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);


        ResponseEntity<User[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                User[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3);



        // Verify MariaDB stored the data correctly
        User[] users = response.getBody();

        assertThat(users[0].getName()).contains("MariaDB User");
        assertThat(users[1].getName()).contains("MariaDB User");
        assertThat(users[2].getName()).contains("MariaDB User");
    }

    @Test
    void testMariaDBSpecificFeatures() {
        // Test MariaDB specific functionality
        User user1 = new User();
        user1.setName("Test User one");
        user1.setEmail("test1@mariadb.com");
        user1.setPassword("123");
        user1.setRole("admin");
        User user2 = new User();
        user2.setName("Test User two");
        user2.setEmail("test2@mariadb.com");
        user2.setPassword("123");
        user2.setRole("admin");
        userRepository.save(user1);
        userRepository.save(user2);

        // Test custom query methods
        assertThat(userRepository.existsByEmail("test1@mariadb.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@mariadb.com")).isFalse();

        // Test findByEmail method
        assertThat(userRepository.findByEmail("test1@mariadb.com")).isPresent();
        assertThat(userRepository.findByEmail("nonexistent@mariadb.com")).get().isNotNull();

        // Test count functionality
        long totalUsers = userRepository.count();
        assertThat(totalUsers).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testMariaDBTransactionHandling() {
        // Test transaction rollback behavior with MariaDB
        long initialCount = userRepository.count();

        try {
            // This should work
            User validUser = new User();
            validUser.setName("Valid User");
            validUser.setEmail("valid@mariadb.com");
            validUser.setPassword("123");
            validUser.setRole("admin");
            userRepository.save(validUser);

            // Verify user was saved
            assertThat(userRepository.count()).isEqualTo(initialCount + 1);

            // Test finding the user
            assertThat(userRepository.findByEmail("valid@mariadb.com")).isPresent();

        } catch (Exception e) {
            // If there's an error, count should remain unchanged
            assertThat(userRepository.count()).isEqualTo(initialCount);
        }
    }

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
}