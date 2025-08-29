package com.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Username must contain only letters and spaces")
    @NotBlank
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String name;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Email(message = "Email must be a valid format")
    @Size(min = 5, max = 254, message = "Email must be between 5 and 254 characters")
    private String email;

    // Default constructor
    public User() {}

    // Constructor with parameters
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}