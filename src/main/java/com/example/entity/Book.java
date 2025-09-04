package com.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotBlank
    private String title;
    
    @Column(nullable = false)
    @NotBlank
    private String author;
    
    @Column(unique = true, nullable = false)
    @NotBlank
    private String isbn;

    @Column(nullable = true)
    private String genre;

    @Column(nullable = true)
    private String description;

    @Column(nullable = true)
    private String price;

    @Column(nullable = false)
    private Integer publishedYear;

    public Book(String title, String author, String isbn) {
        this.title = title;
        this.author = author;    
        this.isbn = isbn;
    }
}