import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { UserListComponent } from './components/user-list/user-list.component';
import { BookListComponent } from './components/book-list/book-list.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'users', component: UserListComponent, canActivate: [authGuard] },
  { path: 'books', component: BookListComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '/login' }
];
