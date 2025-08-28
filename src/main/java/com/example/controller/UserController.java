package com.example.controller;

import com.example.entity.User;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        // Try to get from Redis cache first
        User cachedUser = (User) redisTemplate.opsForValue().get("user:" + id);
        if (cachedUser != null) {
            return ResponseEntity.ok(cachedUser);
        }

        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            // Cache the user in Redis
            redisTemplate.opsForValue().set("user:" + id, user.get());
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        // Cache the new user
        redisTemplate.opsForValue().set("user:" + savedUser.getId(), savedUser);
        return savedUser;
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setName(userDetails.getName());
            user.setEmail(userDetails.getEmail());
            User updatedUser = userRepository.save(user);

            // Update cache
            redisTemplate.opsForValue().set("user:" + id, updatedUser);
            return ResponseEntity.ok(updatedUser);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            // Remove from cache
            redisTemplate.delete("user:" + id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application is running with Docker Compose integration!");
    }
}