package com.example.controller;

import com.example.entity.Book;
import com.example.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
@CrossOrigin(origins = "*")
@Slf4j
public class BookController {

    private final BookService bookService;

    @Operation(summary = "Get all books", description = "Fetches all books from the database")
    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        log.info("Fetching all books from database");
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @Operation(summary = "Get book by ID", description = "Fetches a book by ID, with Redis caching")
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        log.info("Fetching book with id: {}", id);
        return Optional.ofNullable(bookService.getBookById(id))
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new book", description = "Creates a book and caches it in Redis")
    @PostMapping
    public ResponseEntity<Book> createBook(@Valid @RequestBody Book book) {
        log.info("Creating new book: {}", book.getTitle());
        return ResponseEntity.ok(bookService.createBook(book));
    }

    @Operation(summary = "Update book", description = "Updates an existing book and refreshes cache")
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @Valid @RequestBody Book bookDetails) {
        log.info("Updating book with id: {}", id);
        return ResponseEntity.ok(bookService.updateBook(id, bookDetails));
    }

    @Operation(summary = "Delete book", description = "Deletes a book and removes it from cache")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        log.info("Deleting book with id: {}", id);
        return ResponseEntity.ok(bookService.deleteBook(id));
    }

    @Operation(summary = "Search books", description = "Searches books from the database")
    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(@RequestParam("q") String query) {
        log.info("Searching books from database");
        return ResponseEntity.ok(bookService.searchBooks(query));
    }

    @Operation(summary = "Search details", description = "Searches details from ai")
    @GetMapping("/details")
    public ResponseEntity<String> searchDetails(@RequestParam("q") String query) {
        log.info("Searching details from ai");
        return query.startsWith("isbn") ?
                    ResponseEntity.ok("ISBN")
                    : ResponseEntity.ok("Description");
    }

    @Operation(summary = "Health check", description = "Returns application health status")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("Health check requested");
        return ResponseEntity.ok("Application is running with Docker Compose integration!");
    }
}