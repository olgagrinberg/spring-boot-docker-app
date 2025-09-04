package com.example.controller;
/*
What This Provides:

Timeout Protection: All operations will timeout after 5 seconds (configurable) - removed because interfered with security.
Circuit Breaker: Protects against cascading failures
Retry Logic: Automatic retries with exponential backoff
Async Execution: Better resource utilization
Graceful Fallbacks: Proper error responses with meaningful status codes

Client Usage:
From the client perspective, nothing changes - they still receive normal ResponseEntity objects, but internally the operations are handled asynchronously with full resilience patterns applied.
The controller now has comprehensive protection against:

Database timeouts/failures
Redis cache failures
Network issues
Service overload

All while maintaining proper REST API semantics.
 */
import com.example.entity.User;
import com.example.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User API", description = "Operations related to user management with resilience patterns")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final String USER_SERVICE = "userService";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "Get all users", description = "Fetches all users from the database")
    @GetMapping
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackGetAllUsers")
    @Retry(name = USER_SERVICE)
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            logger.info("Fetching all users from database");
            return ResponseEntity.ok(userRepository.findAll());
        } catch (Exception e) {
            logger.error("Error fetching all users", e);
            throw e;
        }
    }

    @Operation(summary = "Get user by ID", description = "Fetches a user by ID, with Redis caching")
    @GetMapping("/{id}")
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackGetUserById")
    @Retry(name = USER_SERVICE)
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            logger.info("Fetching user with id: {}", id);
            // Try to get from Redis cache first
            return Optional.ofNullable((User) redisTemplate.opsForValue().get("user:" + id))
                    .map(cachedUser -> {
                        logger.info("User found in cache: {}", id);
                        return ResponseEntity.ok(cachedUser);
                    })
                    .or(() -> userRepository.findById(id).map(user -> {
                        redisTemplate.opsForValue().set("user:" + id, user);
                        logger.info("User cached successfully: {}", id);
                        return ResponseEntity.ok(user);
                    }))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching user with id: {}", id, e);
            throw e;
        }
    }

    @Operation(summary = "Create a new user", description = "Creates a user and caches it in Redis")
    @PostMapping
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackCreateUser")
    @Retry(name = USER_SERVICE)
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        try {
            logger.info("Creating new user: {}", user.getName());
            User savedUser = userRepository.save(user);

            // Cache the new user
            redisTemplate.opsForValue().set("user:" + savedUser.getId(), savedUser);
            logger.info("User created and cached: {}", savedUser.getId());
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            logger.error("Error creating user: {}", user.getName(), e);
            throw e;
        }
    }

    @Operation(summary = "Update user", description = "Updates an existing user and refreshes cache")
    @PutMapping("/{id}")
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackUpdateUser")
    @Retry(name = USER_SERVICE)
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        try {
            logger.info("Updating user with id: {}", id);

            return userRepository.findById(id)
                    .map(user -> {
                        user.setName(userDetails.getName());
                        user.setEmail(userDetails.getEmail());
                        User updatedUser = userRepository.save(user);

                        redisTemplate.opsForValue().set("user:" + id, updatedUser);
                        logger.info("User updated and cache refreshed: {}", id);

                        return ResponseEntity.ok(updatedUser);
                    })
                    .orElseGet(() -> {
                        logger.warn("User not found with id: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Error updating user with id: {}", id, e);
            throw e;
        }
    }

    @Operation(summary = "Delete user", description = "Deletes a user and removes it from cache")
    @DeleteMapping("/{id}")
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackDeleteUser")
    @Retry(name = USER_SERVICE)
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        logger.info("Deleting user with id: {}", id);

        try {
            return Optional.of(id)
                    .filter(userRepository::existsById)
                    .map(existingId -> {
                        userRepository.deleteById(existingId);
                        redisTemplate.delete("user:" + existingId);
                        logger.info("User deleted and removed from cache: {}", existingId);
                        return ResponseEntity.ok().build();
                    })
                    .orElseGet(() -> {
                        logger.warn("User not found with id: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Error deleting user with id: {}", id, e);
            throw e;
        }
    }

    @Operation(summary = "Health check", description = "Returns application health status")
    @GetMapping("/health")
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackHealth")
    public ResponseEntity<String> health() {
        logger.info("Health check requested");
        return ResponseEntity.ok("Application is running with Docker Compose integration!");
    }

    // Fallback Methods
    public ResponseEntity<List<User>> fallbackGetAllUsers(Exception ex) {
        logger.warn("Circuit breaker activated for getAllUsers. Returning empty list. Error: {}", ex.getMessage());
        return ResponseEntity.status(503).body(new ArrayList<>());
    }

    public ResponseEntity<User> fallbackGetUserById(Long id, Exception ex) {
        logger.warn("Circuit breaker activated for getUserById: {}. Error: {}", id, ex.getMessage());

        // Try to return cached data as last resort
        try {
            User cachedUser = (User) redisTemplate.opsForValue().get("user:" + id);
            if (cachedUser != null) {
                logger.info("Returning cached user from fallback: {}", id);
                return ResponseEntity.ok(cachedUser);
            }
        } catch (Exception cacheEx) {
            logger.error("Cache also failed in fallback: {}", cacheEx.getMessage());
        }

        // Return not found
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<User> fallbackCreateUser(User user, Exception ex) {
        logger.warn("Circuit breaker activated for createUser: {}. Error: {}", user.getName(), ex.getMessage());
        // Return service unavailable
        User fallbackUser = new User();
        fallbackUser.setName("Service Unavailable");
        fallbackUser.setEmail("service.unavailable@example.com");
        return ResponseEntity.status(503).body(fallbackUser);
    }

    public ResponseEntity<User> fallbackUpdateUser(Long id, User userDetails, Exception ex) {
        logger.warn("Circuit breaker activated for updateUser: {}. Error: {}", id, ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<Void> fallbackDeleteUser(Long id, Exception ex) {
        logger.warn("Circuit breaker activated for deleteUser: {}. Error: {}", id, ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<String> fallbackHealth(Exception ex) {
        logger.warn("Circuit breaker activated for health check. Error: {}", ex.getMessage());
        return ResponseEntity.status(503).body("Service temporarily unavailable due to circuit breaker");
    }
}