package com.example.service;

import com.example.entity.Book;
import com.example.repository.BookRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "app:books")
public class BookService {
    private static final String BOOK_SERVICE = "bookService";

    private final BookRepository bookRepository;

    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackGetAllBooks")
    @Retry(name = BOOK_SERVICE)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackGetBookById")
    @Retry(name = BOOK_SERVICE)
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + id));
    }

    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackCreateBook")
    @Retry(name = BOOK_SERVICE)
    public Book createBook(Book book) {
        Book savedBook = bookRepository.save(book);
        log.info("Book created and cached: {}", savedBook.getId());
        return savedBook;
    }

    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackUpdateBook")
    @Retry(name = BOOK_SERVICE)
    public Book updateBook(Long id, Book bookDetails) {
        Book book = getBookById(id);
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setIsbn(bookDetails.getIsbn());
        book.setGenre(bookDetails.getGenre());
        book.setDescription(bookDetails.getDescription());
        book.setPublishedYear(bookDetails.getPublishedYear());
        Book updatedBook = bookRepository.save(book);
        log.info("Book updated and cache refreshed: {}", id);
        return updatedBook;
    }

    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackDeleteBook")
    @Retry(name = BOOK_SERVICE)
    public Book deleteBook(Long id) {
        Book book = getBookById(id);
        bookRepository.deleteById(id);
        log.info("Book deleted and removed from cache: {}", id);
        return book;
    }

    @CircuitBreaker(name = BOOK_SERVICE, fallbackMethod = "fallbackSearchBooks")
    @Retry(name = BOOK_SERVICE)
    public List<Book> searchBooks(String query) {
        try {
            return bookRepository.findBySearchTerm(query)
                    .orElseThrow(() -> new EntityNotFoundException("Books not found with query: " + query));
        } catch (Exception e) {
            log.error("Error searching books", e);
            throw e;
        }
    }

    // Fallback Methods
    public ResponseEntity<List<Book>> fallbackGetAllBooks(Exception ex) {
        log.warn("Circuit breaker activated for getAllBooks. Returning empty list. Error: {}", ex.getMessage());
        return ResponseEntity.status(503).body(new ArrayList<>());
    }

    public ResponseEntity<Book> fallbackGetBookById(Long id, Exception ex) {
        log.warn("Circuit breaker activated for getBookById: {}. Error: {}", id, ex.getMessage());
     return ResponseEntity.status(503).build();
   }

    public ResponseEntity<Book> fallbackCreateBook(Book book, Exception ex) {
        log.warn("Circuit breaker activated for createBook: {}. Error: {}", book.getTitle(), ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<Book> fallbackUpdateBook(Long id, Book bookDetails, Exception ex) {
        log.warn("Circuit breaker activated for updateBook: {}. Error: {}", id, ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<Void> fallbackDeleteBook(Long id, Exception ex) {
        log.warn("Circuit breaker activated for deleteBook: {}. Error: {}", id, ex.getMessage());
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<List<Book>> fallbackSearchBooks(String search, Exception ex) {
        log.warn("Circuit breaker activated for searchBooks: {}. Error: {}", search, ex.getMessage());
        return ResponseEntity.status(503).build();
    };

    public ResponseEntity<String> fallbackSearchDetails(String search, Exception ex) {
        log.warn("Circuit breaker activated for searchDetails: {}. Error: {}", search, ex.getMessage());
        return ResponseEntity.status(503).build();
    };


}
