import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { User } from '../../models/user.model';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.css'
})
export class UserListComponent implements OnInit {
  users: User[] = [];
  isLoading: boolean = false;
  errorMessage: string = '';
  selectedUser: User | null = null;
  showModal: boolean = false;
  isEditing: boolean = false;

  newUser: User = {
    name: '',
    email: ''
  };

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.errorMessage = 'Failed to load users. Please try again.';
        this.isLoading = false;
      }
    });
  }

  openCreateModal(): void {
    this.selectedUser = null;
    this.newUser = { name: '', email: '' };
    this.isEditing = false;
    this.showModal = true;
  }

  openEditModal(user: User): void {
    this.selectedUser = { ...user };
    this.newUser = { ...user };
    this.isEditing = true;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedUser = null;
    this.newUser = { name: '', email: '' };
    this.isEditing = false;
  }

  saveUser(): void {
    if (!this.newUser.name || !this.newUser.email) {
      return;
    }

    if (this.isEditing && this.selectedUser?.id) {
      this.userService.updateUser(this.selectedUser.id, this.newUser).subscribe({
        next: () => {
          this.closeModal();
          this.loadUsers();
        },
        error: (error) => {
          console.error('Error updating user:', error);
          this.errorMessage = 'Failed to update user.';
        }
      });
    } else {
      this.userService.createUser(this.newUser).subscribe({
        next: () => {
          this.closeModal();
          this.loadUsers();
        },
        error: (error) => {
          console.error('Error creating user:', error);
          this.errorMessage = 'Failed to create user.';
        }
      });
    }
  }

  deleteUser(user: User): void {
    if (!user.id || !confirm(`Are you sure you want to delete ${user.name}?`)) {
      return;
    }

    this.userService.deleteUser(user.id).subscribe({
      next: () => {
        this.loadUsers();
      },
      error: (error) => {
        console.error('Error deleting user:', error);
        this.errorMessage = 'Failed to delete user.';
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
