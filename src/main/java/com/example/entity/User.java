package com.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name"),
        @UniqueConstraint(columnNames = "email")})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Pattern(regexp = "^[A-Za-z ]+$", message = "Username must contain only letters and spaces")
    @NotBlank
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String name;

    @NotBlank
    @Email(message = "Email must be a valid format")
    @Size(min = 5, max = 254, message = "Email must be between 5 and 254 characters")
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @NotBlank
    @Size(max = 20)
    private String role;

    @Transient
    private String operation;
}