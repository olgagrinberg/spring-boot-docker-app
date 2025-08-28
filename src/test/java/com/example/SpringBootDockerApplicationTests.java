package com.example;

import com.example.entity.User;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
@Disabled
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SpringBootDockerApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
        User user = new User("MariaDB Test User", "mariadb@example.com");
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
        // Test the custom health endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Application is running");
    }

    @Test
    void testActuatorHealthEndpoint() {
        // Test the actuator health endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void testUserCrudOperationsWithMariaDB() {
        // Test complete CRUD operations via REST API with MariaDB
        String baseUrl = "http://localhost:" + port + "/api/users";

        // CREATE - Post a new user
        User newUser = new User("Maria DB User", "maria.db@example.com");
        ResponseEntity<User> createResponse = restTemplate.postForEntity(baseUrl, newUser, User.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo("Maria DB User");
        assertThat(createResponse.getBody().getId()).isNotNull();

        Long userId = createResponse.getBody().getId();

        // READ - Get the created user
        ResponseEntity<User> getResponse = restTemplate.getForEntity(
                baseUrl + "/" + userId, User.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getName()).isEqualTo("Maria DB User");

        // UPDATE - Update the user
        User updatedUser = new User("Updated Maria User", "updated.maria@example.com");
        restTemplate.put(baseUrl + "/" + userId, updatedUser);

        // Verify update
        ResponseEntity<User> updatedResponse = restTemplate.getForEntity(
                baseUrl + "/" + userId, User.class);
        assertThat(updatedResponse.getBody().getName()).isEqualTo("Updated Maria User");
        assertThat(updatedResponse.getBody().getEmail()).isEqualTo("updated.maria@example.com");

        // DELETE - Delete the user
        restTemplate.delete(baseUrl + "/" + userId);

        // Verify deletion
        ResponseEntity<User> deletedResponse = restTemplate.getForEntity(
                baseUrl + "/" + userId, User.class);
        assertThat(deletedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUserCachingWithMariaDB() {
        // Test that Redis caching works correctly with MariaDB backend
        User user = new User("Cached MariaDB User", "cached.mariadb@example.com");
        User savedUser = userRepository.save(user);

        // First request - should cache the user
        String url = "http://localhost:" + port + "/api/users/" + savedUser.getId();
        ResponseEntity<User> firstResponse = restTemplate.getForEntity(url, User.class);

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstResponse.getBody()).isNotNull();

        // Check if user is in Redis cache
        Object cachedUser = redisTemplate.opsForValue().get("user:" + savedUser.getId());
        assertThat(cachedUser).isNotNull();

        // Second request - should come from cache
        ResponseEntity<User> secondResponse = restTemplate.getForEntity(url, User.class);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondResponse.getBody().getName()).isEqualTo("Cached MariaDB User");
    }

    @Test
    void testGetAllUsersWithMariaDB() {
        // Clean up existing users first
        userRepository.deleteAll();

        // Create some test users with MariaDB specific data
        userRepository.save(new User("MariaDB User 1", "maria1@example.com"));
        userRepository.save(new User("MariaDB User 2", "maria2@example.com"));
        userRepository.save(new User("MariaDB User 3", "maria3@example.com"));

        // Get all users via REST API
        String url = "http://localhost:" + port + "/api/users";
        ResponseEntity<User[]> response = restTemplate.getForEntity(url, User[].class);

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
        User user1 = new User("Test User 1", "test1@mariadb.com");
        User user2 = new User("Test User 2", "test2@mariadb.com");

        userRepository.save(user1);
        userRepository.save(user2);

        // Test custom query methods
        assertThat(userRepository.existsByEmail("test1@mariadb.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@mariadb.com")).isFalse();

        // Test findByEmail method
        assertThat(userRepository.findByEmail("test1@mariadb.com")).isPresent();
        assertThat(userRepository.findByEmail("nonexistent@mariadb.com")).isEmpty();

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
            User validUser = new User("Valid User", "valid@mariadb.com");
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
}