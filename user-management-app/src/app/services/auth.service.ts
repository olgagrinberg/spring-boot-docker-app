import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map } from 'rxjs/operators';
import { LoginRequest, AuthResponse, User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api';
  private tokenSubject = new BehaviorSubject<string | null>(null);
  private userSubject = new BehaviorSubject<User | null>(null);

  public token$ = this.tokenSubject.asObservable();
  public user$ = this.userSubject.asObservable();

  constructor(private http: HttpClient) {
    // Check for existing token on service initialization
    const token = localStorage.getItem('token');
    if (token) {
      this.tokenSubject.next(token);
    }
  }

  login(credentials: LoginRequest): Observable<boolean> {
    // Hardcoded login validation
    if (credentials.username === 'admin' && credentials.password === 'admin') {
      // Simulate API response
      const mockToken = 'mock-jwt-token-' + Date.now();
      const mockUser: User = { id: 1, name: 'Admin', email: 'admin@example.com' };

      localStorage.setItem('token', mockToken);
      this.tokenSubject.next(mockToken);
      this.userSubject.next(mockUser);

      return new Observable(observer => {
        observer.next(true);
        observer.complete();
      });
    }

    return new Observable(observer => {
      observer.next(false);
      observer.complete();
    });
  }

  logout(): void {
    localStorage.removeItem('token');
    this.tokenSubject.next(null);
    this.userSubject.next(null);
  }

  isAuthenticated(): boolean {
    return this.tokenSubject.value !== null;
  }

  getToken(): string | null {
    return this.tokenSubject.value;
  }

  private getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }
}
