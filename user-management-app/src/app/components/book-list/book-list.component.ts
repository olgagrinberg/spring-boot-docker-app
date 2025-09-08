import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Book } from '../../models/book.model';
import { BookService } from '../../services/book.service';
import { AuthService } from '../../services/auth.service';
import {User} from '../../models/user.model';

@Component({
  selector: 'app-book-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './book-list.component.html',
  styleUrl: './book-list.component.css'
})
export class BookListComponent implements OnInit {
  books: Book[] = [];
  isLoading: boolean = false;
  errorMessage: string = '';
  selectedBook: Book | null = null;
  showModal: boolean = false;
  isEditing: boolean = false;
  searchQuery: string = '';

  newBook: Book = {
    title: '',
    author: '',
    isbn: '',
    publishedYear: new Date().getFullYear(),
    genre: '',
    description: ''
  };

  genres: string[] = [
    'Fiction',
    'Non-Fiction',
    'Science Fiction',
    'Fantasy',
    'Mystery',
    'Romance',
    'Thriller',
    'Biography',
    'History',
    'Self-Help',
    'Technology',
    'Business',
    'Other'
  ];

  // Pagination properties
  currentPage: number = 1;
  itemsPerPage: number = 20;
  totalItems: number = 0;
  totalPages: number = 0;
  paginatedBooks: Book[] = [];
  allBooks: Book[] = []; // Store all books for filtering
  filteredBooks: Book[] = []; // Store filtered books
  totalBooksBeforeFilter: number = 0;

  constructor(
    private bookService: BookService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadBooks();
  }

  loadBooks(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.bookService.getAllBooks().subscribe({
      next: (books) => {
        this.allBooks = books;
        this.totalBooksBeforeFilter = books.length;
        this.applyFilterAndPagination();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading books:', error);
        this.errorMessage = 'Failed to load books. Please try again.';
        this.isLoading = false;
      }
    });
  }

  applyFilterAndPagination(): void {
    // Apply search filter
    if (this.searchQuery.trim()) {
      this.filteredBooks = this.allBooks.filter(book =>
        book.title.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        book.author.toLowerCase().includes(this.searchQuery.toLowerCase())
      );
    } else {
      this.filteredBooks = [...this.allBooks];
    }

    this.totalItems = this.filteredBooks.length;
    this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);

    // Reset to first page if current page is beyond available pages
    if (this.currentPage > this.totalPages && this.totalPages > 0) {
      this.currentPage = 1;
    }

    // Apply pagination
    this.updatePaginatedBooks();
  }

  updatePaginatedBooks(): void {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.paginatedBooks = this.filteredBooks.slice(startIndex, endIndex);
    this.books = this.paginatedBooks; // Keep backward compatibility with template
  }

  // Pagination methods
  onPageChange(page: number): void {
    this.currentPage = page;
    this.updatePaginatedBooks();
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePaginatedBooks();
    }
  }

  onItemsPerPageChange(): void {
    this.currentPage = 1;
    this.applyFilterAndPagination();
  }

  onSearchChange(): void {
    this.currentPage = 1;
    this.applyFilterAndPagination();
  }

  searchUsers(): void {
    this.currentPage = 1;
    this.applyFilterAndPagination();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.currentPage = 1;
    this.applyFilterAndPagination();
  }

  // Helper methods for pagination info
  getStartIndex(): number {
    if (this.totalItems === 0) return 0;
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  getEndIndex(): number {
    const endIndex = this.currentPage * this.itemsPerPage;
    return endIndex > this.totalItems ? this.totalItems : endIndex;
  }

  getVisiblePages(): number[] {
    const maxVisiblePages = 5;
    const pages: number[] = [];

    if (this.totalPages <= maxVisiblePages) {
      for (let i = 1; i <= this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      const half = Math.floor(maxVisiblePages / 2);
      let start = Math.max(1, this.currentPage - half);
      let end = Math.min(this.totalPages, this.currentPage + half);

      if (end - start + 1 < maxVisiblePages) {
        if (start === 1) {
          end = Math.min(this.totalPages, start + maxVisiblePages - 1);
        } else {
          start = Math.max(1, end - maxVisiblePages + 1);
        }
      }

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }
    }

    return pages;
  }

  openCreateModal(): void {
    this.selectedBook = null;
    this.newBook = {
      title: '',
      author: '',
      isbn: '',
      publishedYear: new Date().getFullYear(),
      genre: '',
      description: ''
    };
    this.isEditing = false;
    this.showModal = true;
  }

  openEditModal(book: Book): void {
    this.selectedBook = { ...book };
    this.newBook = { ...book };
    this.isEditing = true;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedBook = null;
    this.newBook = {
      title: '',
      author: '',
      isbn: '',
      publishedYear: new Date().getFullYear(),
      genre: '',
      description: ''
    };
    this.isEditing = false;
  }

  onFindDescription(): void {
    // Check if we have a title or author to search with
    const searchQuery = this.newBook.title || this.newBook.author;

    if (!searchQuery || !searchQuery.trim()) {
      console.warn('No title or author provided for Description search');
      return;
    }

    // Call the book service to search for Description
    this.bookService.searchDetails("descr:"+searchQuery.trim()).subscribe({
      next: (isbn: string) => {
        if (isbn) {
          this.newBook.isbn = isbn;
          console.log('Found Description:', isbn);
        } else {
          console.warn('No Description found for query:', searchQuery);
        }
      },
      error: (error) => {
        console.error('Error searching for Description:', error);
        this.errorMessage = 'Failed to find Description. Please try again.';
      }
    });
  }

  onFindIsbn(): void {
    // Check if we have a title or author to search with
    const searchQuery = this.newBook.title || this.newBook.author;

    if (!searchQuery || !searchQuery.trim()) {
      console.warn('No title or author provided for ISBN search');
      return;
    }

    // Call the book service to search for ISBN
    this.bookService.searchDetails("isbn:"+searchQuery.trim()).subscribe({
      next: (isbn: string) => {
        if (isbn) {
          this.newBook.isbn = isbn;
          console.log('Found ISBN:', isbn);
        } else {
          console.warn('No ISBN found for query:', searchQuery);
        }
      },
      error: (error) => {
        console.error('Error searching for ISBN:', error);
        this.errorMessage = 'Failed to find ISBN. Please try again.';
      }
    });
  }

  saveBook(): void {
    if (!this.newBook.title || !this.newBook.author || !this.newBook.isbn) {
      return;
    }

    if (this.isEditing && this.selectedBook?.id) {
      this.bookService.updateBook(this.selectedBook.id, this.newBook).subscribe({
        next: () => {
          this.closeModal();
          this.loadBooks();
        },
        error: (error) => {
          console.error('Error updating book:', error);
          this.errorMessage = 'Failed to update book.';
        }
      });
    } else {
      this.bookService.createBook(this.newBook).subscribe({
        next: () => {
          this.closeModal();
          this.loadBooks();
        },
        error: (error) => {
          console.error('Error creating book:', error);
          this.errorMessage = 'Failed to create book.';
        }
      });
    }
  }

  deleteBook(book: Book): void {
    if (!book.id || !confirm(`Are you sure you want to delete "${book.title}"?`)) {
      return;
    }

    this.bookService.deleteBook(book.id).subscribe({
      next: () => {
        // Remove book from local arrays for immediate UI update
        this.allBooks = this.allBooks.filter(u => u.id !== book.id);
        this.totalBooksBeforeFilter = this.allBooks.length;
        this.applyFilterAndPagination();

        // Alternative: Call loadUsers() to refresh from server
        // this.loadUsers();
      },
      error: (error) => {
        console.error('Error deleting book:', error);
        this.errorMessage = 'Failed to delete book.';
      }
    });
  }

  navigateToUsers(): void {
    this.router.navigate(['/users']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
