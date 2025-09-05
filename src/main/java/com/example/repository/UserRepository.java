package com.example.repository;

import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<List<User>> findByName(String name);

    boolean existsByName(String name);

    Optional<List<User>> findByEmail(String email);

    boolean existsByEmail(String email);
}