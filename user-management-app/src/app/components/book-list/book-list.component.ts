import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Book } from '../../models/book.model';
import { BookService } from '../../services/book.service';
import { AuthService } from '../../services/auth.service';

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
        this.books = books;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading books:', error);
        this.errorMessage = 'Failed to load books. Please try again.';
        this.isLoading = false;
      }
    });
  }

  searchBooks(): void {
    if (this.searchQuery.trim()) {
      this.isLoading = true;
      this.bookService.searchBooks(this.searchQuery).subscribe({
        next: (books) => {
          this.books = books;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error searching books:', error);
          this.errorMessage = 'Failed to search books. Please try again.';
          this.isLoading = false;
        }
      });
    } else {
      this.loadBooks();
    }
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.loadBooks();
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
        this.loadBooks();
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
