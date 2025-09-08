import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Book } from '../models/book.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class BookService {
  private baseUrl = 'http://localhost:8080/api/books';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  getAllBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(this.baseUrl, {
      headers: this.getAuthHeaders()
    });
  }

  getBookById(id: number): Observable<Book> {
    return this.http.get<Book>(`${this.baseUrl}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  createBook(book: Book): Observable<Book> {
    return this.http.post<Book>(this.baseUrl, book, {
      headers: this.getAuthHeaders()
    });
  }

  updateBook(id: number, book: Book): Observable<Book> {
    return this.http.put<Book>(`${this.baseUrl}/${id}`, book, {
      headers: this.getAuthHeaders()
    });
  }

  deleteBook(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  searchBooks(query: string): Observable<Book[]> {
    return this.http.get<Book[]>(`${this.baseUrl}/search?q=${query}`, {
      headers: this.getAuthHeaders()
    });
  }

  searchDetails(query: string): Observable<string> {
    return this.http.get<string>(`${this.baseUrl}/searchDetails?q=${query}`, {
    headers: this.getAuthHeaders()
    });
  }

  getBooksByGenre(genre: string): Observable<Book[]> {
    return this.http.get<Book[]>(`${this.baseUrl}/genre/${genre}`, {
      headers: this.getAuthHeaders()
    });
  }
}
