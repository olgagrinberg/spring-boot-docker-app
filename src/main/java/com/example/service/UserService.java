package com.example.service;

import com.example.entity.User;
import com.example.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "app:users")
public class UserService {

    private static final String USER_SERVICE = "userService";

    private final UserRepository userRepository;

    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackGetAllUsers")
    @Retry(name = USER_SERVICE)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackGetUserById")
    @Retry(name = USER_SERVICE)
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        log.info("User {} loaded from database and cached", id);
        return user;
    }

    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackCreateUser")
    @Retry(name = USER_SERVICE)
    @CachePut(value = "users", key = "#user.id")
    public User createUser(User user) {
        User savedUser = userRepository.save(user);
        log.info("User created and cached: {}", savedUser.getId());
        return savedUser;
    }

    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackUpdateUser")
    @Retry(name = USER_SERVICE)
    @Caching(
            evict = {@CacheEvict(value = "users", key = "#id")},
            put = {@CachePut(value = "users", key = "#id")}
    )
    public User updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        log.info("User updated and cache refreshed: {}", id);
        return userRepository.save(existingUser);
    }

    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackDeleteUser")
    @Retry(name = USER_SERVICE)
    @CacheEvict(value = "users", key = "#id")
    public Long deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        userRepository.deleteById(id);
        log.info("User deleted and removed from cache: {}", id);
        return user.getId();
    }

    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackSearchUsers")
    @Retry(name = USER_SERVICE)
    public List<User> searchUsers(@RequestParam("q") String query) {
        return userRepository.findByName(query)
                .orElseThrow(() -> new EntityNotFoundException("Users not found with query: " + query));
    }

    // Fallback Methods
    public ResponseEntity<List<User>> fallbackGetAllUsers(Exception ex) {
        log.warn("Circuit breaker activated for getAllUsers. Returning empty list. Error: {}", ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<User> fallbackGetUserById(Long id, Exception ex) {
        log.warn("Circuit breaker activated for getUserById: {}. Error: {}", id, ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<User> fallbackCreateUser(User user, Exception ex) {
        log.warn("Circuit breaker activated for createUser: {}. Error: {}", user.getName(), ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<User> fallbackUpdateUser(Long id, User userDetails, Exception ex) {
        log.warn("Circuit breaker activated for updateUser: {}. Error: {}", id, ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<Void> fallbackDeleteUser(Long id, Exception ex) {
        log.warn("Circuit breaker activated for deleteUser: {}. Error: {}", id, ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<List<User>> fallbackSearchUsers(String search, Exception ex) {
        log.warn("Circuit breaker activated for searchUsers: {}. Error: {}", search, ex.getMessage());
        return ResponseEntity.status(503).build();
    };
}
