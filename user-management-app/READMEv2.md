# User Management App

This is an Angular 18 application with standalone components that provides user management functionality with authentication.

## Features

- **Authentication**: Login with hardcoded credentials (admin/admin)
- **User Management**: CRUD operations for users
- **Book Management**: CRUD operations for books with search functionality
- **Navigation**: Easy switching between User and Book management
- **Responsive Design**: Bootstrap 5 UI components
- **API Integration**: REST API calls to localhost:8080
- **Authorization**: Bearer token authentication
- **Standalone Components**: Modern Angular architecture

## Project Structure

```
src/
├── app/
│   ├── components/
│   │   ├── login/
│   │   │   ├── login.component.ts
│   │   │   ├── login.component.html
│   │   │   └── login.component.css
│   │   └── user-list/
│   │       ├── user-list.component.ts
│   │       ├── user-list.component.html
│   │       └── user-list.component.css
│   │   └── book-list/
│   │       ├── book-list.component.ts
│   │       ├── book-list.component.html
│   │       └── book-list.component.css
│   ├── models/
│   │   ├── user.model.ts
│   │   └── book.model.ts
│   ├── services/
│   │   ├── auth.service.ts
│   │   ├── user.service.ts
│   │   └── book.service.ts
│   ├── guards/
│   │   └── auth.guard.ts
│   ├── app.component.ts
│   ├── app.component.css
│   ├── app.config.ts
│   └── app.routes.ts
├── index.html
├── main.ts
└── styles.css
```

## API Endpoints

The application is configured to make API calls to `http://localhost:8080` with the following endpoints:

### User Management
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID (with Redis caching)
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Book Management
- `GET /api/books` - Get all books
- `GET /api/books/{id}` - Get book by ID (with Redis caching)
- `POST /api/books` - Create new book
- `PUT /api/books/{id}` - Update book
- `DELETE /api/books/{id}` - Delete book
- `GET /api/books/search?q={query}` - Search books by title, author, or ISBN
- `GET /api/books/genre/{genre}` - Get books by genre

All API calls include Authorization header with Bearer token.

## Installation

1. Install Angular CLI globally:
```bash
npm install -g @angular/cli@18
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
ng serve
```

4. Open your browser and navigate to `http://localhost:4200`

## Usage

1. **Login**: Use credentials `admin/admin` to access the application
2. **Navigation**: Switch between User and Book management using the navigation buttons
3. **User Management**:
  - View Users: See the user management dashboard
  - Add User: Click "Add User" button to create a new user
  - Edit User: Click "Edit" button next to any user to modify their details
  - Delete User: Click "Delete" button to remove a user (with confirmation)
4. **Book Management**:
  - View Books: See the book management dashboard with detailed book information
  - Search Books: Use the search bar to find books by title, author, or ISBN
  - Add Book: Click "Add Book" button to create a new book entry
  - Edit Book: Click "Edit" button next to any book to modify its details
  - Delete Book: Click "Delete" button to remove a book (with confirmation)
5. **Logout**: Click "Logout" to end your session

## Technologies Used

- **Angular 18**: Latest version with standalone components
- **TypeScript**: For type-safe development
- **Bootstrap 5**: For responsive UI components
- **Bootstrap Icons**: For iconography
- **RxJS**: For reactive programming
- **Angular Router**: For navigation
- **Angular Forms**: For form handling
- **HttpClient**: For API communication

## Authentication

The application uses a simple hardcoded authentication system:
- Username: `admin`
- Password: `admin`

Upon successful login, a mock JWT token is stored in localStorage and used for API authorization.

## Components

### Login Component
- Handles user authentication
- Form validation
- Error handling
- Responsive design

### User List Component
- Displays users in a table format
- Add/Edit/Delete functionality
- Modal dialogs for user forms
- Navigation to Book management
- Loading states and error handling

### Book List Component
- Displays books in a detailed table format
- Add/Edit/Delete functionality with comprehensive book information
- Search functionality by title, author, or ISBN
- Genre categorization with predefined options
- Modal dialogs for book forms with validation
- Navigation to User management
- Loading states and error handling

## Services

### AuthService
- Handles login/logout functionality
- Token management
- Authentication state management

### UserService
- CRUD operations for users
- HTTP requests with authorization headers
- Error handling

### BookService
- CRUD operations for books
- Search functionality
- Genre-based filtering
- HTTP requests with authorization headers
- Error handling

## Guards

### AuthGuard
- Protects routes that require authentication
- Redirects to login if not authenticated

## Styling

The application uses Bootstrap 5 for styling with custom CSS enhancements:
- Responsive design
- Modern UI components
- Smooth transitions
- Custom color scheme
- Loading animations

## Development

To add new features or modify existing ones:

1. Follow Angular standalone component patterns
2. Use TypeScript for type safety
3. Implement proper error handling
4. Add appropriate CSS styling
5. Test functionality thoroughly

## Build

To build the project for production:

```bash
ng build --prod
```

The build artifacts will be stored in the `dist/` directory.
