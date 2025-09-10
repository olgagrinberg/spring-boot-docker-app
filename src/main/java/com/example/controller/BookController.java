package com.example.controller;

import com.example.entity.Book;
import com.example.repository.BookRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/books")
@CrossOrigin(origins = "*")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    private static final String BOOK_SERVICE = "bookService";

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "Get all books", description = "Fetches all books from the database")
    @GetMapping
    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackGetAllBooks")
    @Retry(name = BOOK_SERVICE)
    public ResponseEntity<List<Book>> getAllBooks() {
        try {
            logger.info("Fetching all books from database");
            return ResponseEntity.ok(bookRepository.findAll());
        } catch (Exception e) {
            logger.error("Error fetching all books", e);
            throw e;
        }
    }

    @Operation(summary = "Get book by ID", description = "Fetches a book by ID, with Redis caching")
    @GetMapping("/{id}")
    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackGetBookById")
    @Retry(name = BOOK_SERVICE)
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        try {
            logger.info("Fetching book with id: {}", id);
            // Try to get from Redis cache first
            return Optional.ofNullable((Book) redisTemplate.opsForValue().get("book:" + id))
                    .map(cachedBook -> {
                        logger.info("Book found in cache: {}", id);
                        return ResponseEntity.ok(cachedBook);
                    })
                    .or(() -> bookRepository.findById(id).map(book -> {
                        redisTemplate.opsForValue().set("book:" + id, book);
                        logger.info("Book cached successfully: {}", id);
                        return ResponseEntity.ok(book);
                    }))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching book with id: {}", id, e);
            throw e;
        }
    }

    @Operation(summary = "Create a new book", description = "Creates a book and caches it in Redis")
    @PostMapping
    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackCreateBook")
    @Retry(name = BOOK_SERVICE)
    public ResponseEntity<Book> createBook(@Valid @RequestBody Book book) {
        try {
            logger.info("Creating new book: {}", book.getTitle());
            Book savedBook = bookRepository.save(book);

            // Cache the new book
            redisTemplate.opsForValue().set("book:" + savedBook.getId(), savedBook);
            logger.info("Book created and cached: {}", savedBook.getId());
            return ResponseEntity.ok(savedBook);
        } catch (Exception e) {
            logger.error("Error creating book: {}", book.getTitle(), e);
            throw e;
        }
    }

    @Operation(summary = "Update book", description = "Updates an existing book and refreshes cache")
    @PutMapping("/{id}")
    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackUpdateBook")
    @Retry(name = BOOK_SERVICE)
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @Valid @RequestBody Book bookDetails) {
        try {
            logger.info("Updating book with id: {}", id);

            return bookRepository.findById(id)
                    .map(book -> {
                        book.setTitle(bookDetails.getTitle());
                        book.setAuthor(bookDetails.getAuthor());
                        book.setIsbn(bookDetails.getIsbn());
                        book.setGenre(bookDetails.getGenre());
                        book.setDescription(bookDetails.getDescription());
                        book.setPublishedYear(bookDetails.getPublishedYear());
                        Book updatedBook = bookRepository.save(book);

                        redisTemplate.opsForValue().set("book:" + id, updatedBook);
                        logger.info("Book updated and cache refreshed: {}", id);

                        return ResponseEntity.ok(updatedBook);
                    })
                    .orElseGet(() -> {
                        logger.warn("Book not found with id: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Error updating book with id: {}", id, e);
            throw e;
        }
    }

    @Operation(summary = "Delete book", description = "Deletes a book and removes it from cache")
    @DeleteMapping("/{id}")
    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackDeleteBook")
    @Retry(name = BOOK_SERVICE)
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        logger.info("Deleting book with id: {}", id);

        try {
            return Optional.of(id)
                    .filter(bookRepository::existsById)
                    .map(existingId -> {
                        bookRepository.deleteById(existingId);
                        redisTemplate.delete("book:" + existingId);
                        logger.info("Book deleted and removed from cache: {}", existingId);
                        return ResponseEntity.ok().build();
                    })
                    .orElseGet(() -> {
                        logger.warn("Book not found with id: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Error deleting book with id: {}", id, e);
            throw e;
        }
    }

    @Operation(summary = "Search books", description = "Searches books from the database")
    @GetMapping("/search")
    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackSearchBooks")
    @Retry(name = BOOK_SERVICE)
    public ResponseEntity<List<Book>> searchBooks(@RequestParam("q") String query) {
        try {
            logger.info("Searching books from database");
            return bookRepository.findBySearchTerm(query)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        logger.warn("Books not found with query: {}", query);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Error searching books", e);
            throw e;
        }
    }

    @Operation(summary = "Search details", description = "Searches details from ai")
    @GetMapping("/details")
    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackSearchDetails")
    @Retry(name = BOOK_SERVICE)
    public ResponseEntity<String> searchDetails(@RequestParam("q") String query) {
        try {
            logger.info("Searching details from ai");

            return query.startsWith("isbn") ? ResponseEntity.ok("ISBN") : ResponseEntity.ok("Description");

        } catch (Exception e) {
            logger.error("Error searching details", e);
            throw e;
        }
    }

    @Operation(summary = "Health check", description = "Returns application health status")
    @GetMapping("/health")
    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackHealth")
    public ResponseEntity<String> health() {
        logger.info("Health check requested");
        return ResponseEntity.ok("Application is running with Docker Compose integration!");
    }

    // Fallback Methods
    public ResponseEntity<List<Book>> fallbackGetAllBooks(Exception ex) {
        logger.warn("Circuit breaker activated for getAllBooks. Returning empty list. Error: {}", ex.getMessage());
        return ResponseEntity.status(503).body(new ArrayList<>());
    }

    public ResponseEntity<Book> fallbackGetBookById(Long id, Exception ex) {
        logger.warn("Circuit breaker activated for getBookById: {}. Error: {}", id, ex.getMessage());

        // Try to return cached data as last resort
        try {
            Book cachedBook = (Book) redisTemplate.opsForValue().get("book:" + id);
            if (cachedBook != null) {
                logger.info("Returning cached book from fallback: {}", id);
                return ResponseEntity.ok(cachedBook);
            }
        } catch (Exception cacheEx) {
            logger.error("Cache also failed in fallback: {}", cacheEx.getMessage());
        }

        // Return not found
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Book> fallbackCreateBook(Book book, Exception ex) {
        logger.warn("Circuit breaker activated for createBook: {}. Error: {}", book.getTitle(), ex.getMessage());
        // Return service unavailable
        Book fallbackBook = new Book();
        fallbackBook.setTitle("Service Unavailable");
        return ResponseEntity.status(503).body(fallbackBook);
    }

    public ResponseEntity<Book> fallbackUpdateBook(Long id, Book bookDetails, Exception ex) {
        logger.warn("Circuit breaker activated for updateBook: {}. Error: {}", id, ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<Void> fallbackDeleteBook(Long id, Exception ex) {
        logger.warn("Circuit breaker activated for deleteBook: {}. Error: {}", id, ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<List<Book>> fallbackSearchBooks(String search, Exception ex) {
        logger.warn("Circuit breaker activated for searchBooks: {}. Error: {}", search, ex.getMessage());
        return ResponseEntity.status(503).build();
    };

    public ResponseEntity<String> fallbackSearchDetails(String search, Exception ex) {
        logger.warn("Circuit breaker activated for searchDetails: {}. Error: {}", search, ex.getMessage());
        return ResponseEntity.status(503).build();
    };

    public ResponseEntity<String> fallbackHealth(Exception ex) {
        logger.warn("Circuit breaker activated for health check. Error: {}", ex.getMessage());
        return ResponseEntity.status(503).body("Service temporarily unavailable due to circuit breaker");
    }
}