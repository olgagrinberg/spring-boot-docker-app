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
  searchQuery: string = '';

  newUser: User = {
    name: '',
    email: ''
  };

  // Pagination properties
  currentPage: number = 1;
  itemsPerPage: number = 20;
  totalItems: number = 0;
  totalPages: number = 0;
  paginatedUsers: User[] = [];
  allUsers: User[] = []; // Store all users for filtering
  filteredUsers: User[] = []; // Store filtered users
  totalUsersBeforeFilter: number = 0;

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
        this.allUsers = users;
        this.totalUsersBeforeFilter = users.length;
        this.applyFilterAndPagination();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.errorMessage = 'Failed to load users. Please try again.';
        this.isLoading = false;
      }
    });
  }

  applyFilterAndPagination(): void {
    // Apply search filter
    if (this.searchQuery.trim()) {
      this.filteredUsers = this.allUsers.filter(user =>
        user.name.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        user.email.toLowerCase().includes(this.searchQuery.toLowerCase())
      );
    } else {
      this.filteredUsers = [...this.allUsers];
    }

    this.totalItems = this.filteredUsers.length;
    this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);

    // Reset to first page if current page is beyond available pages
    if (this.currentPage > this.totalPages && this.totalPages > 0) {
      this.currentPage = 1;
    }

    // Apply pagination
    this.updatePaginatedUsers();
  }

  updatePaginatedUsers(): void {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.paginatedUsers = this.filteredUsers.slice(startIndex, endIndex);
    this.users = this.paginatedUsers; // Keep backward compatibility with template
  }

  // Pagination methods
  onPageChange(page: number): void {
    this.currentPage = page;
    this.updatePaginatedUsers();
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePaginatedUsers();
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
          this.loadUsers(); // This will refresh the pagination
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
          this.loadUsers(); // This will refresh the pagination
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
        // Remove user from local arrays for immediate UI update
        this.allUsers = this.allUsers.filter(u => u.id !== user.id);
        this.totalUsersBeforeFilter = this.allUsers.length;
        this.applyFilterAndPagination();

        // Alternative: Call loadUsers() to refresh from server
        // this.loadUsers();
      },
      error: (error) => {
        console.error('Error deleting user:', error);
        this.errorMessage = 'Failed to delete user.';
      }
    });
  }

  navigateToBooks(): void {
    this.router.navigate(['/books']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
