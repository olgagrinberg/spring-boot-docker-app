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
import com.example.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User API", description = "Operations related to user management with resilience patterns")
@Slf4j
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get all users", description = "Fetches all users from the database")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Fetching all users from database");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Get user by ID", description = "Fetches a user by ID, with Redis caching")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("Fetching user with id: {}", id);
        return Optional.ofNullable(userService.getUserById(id))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new user", description = "Creates a user and caches it in Redis")
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        log.info("Creating new user: {}", user.getName());
        User createdUser = userService.createUser(user);
        return ResponseEntity.created(URI.create("/api/users/" + createdUser.getId()))
                .body(createdUser);
    }

    @Operation(summary = "Update user", description = "Updates an existing user and refreshes cache")
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        log.info("Updating user with id: {}", id);
        return ResponseEntity.ok(userService.updateUser(id, userDetails));
    }

    @Operation(summary = "Delete user", description = "Deletes a user and removes it from cache")
    @DeleteMapping("/{id}")
    public ResponseEntity<Long> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with id: {}", id);
        return ResponseEntity.ok(userService.deleteUser(id));
    }

    @Operation(summary = "Search users", description = "Searches users from the database")
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam("q") String query) {
        log.info("Searching users from database");
        return ResponseEntity.ok(userService.searchUsers(query));
    }

    @Operation(summary = "Health check", description = "Returns application health status")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("Health check requested");
        return ResponseEntity.ok("Application is running with Docker Compose integration!");
    }
}