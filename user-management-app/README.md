# User Management App

This is an Angular 18 application with standalone components that provides user management functionality with authentication.

## Features

- **Authentication**: Login with hardcoded credentials (admin/admin)
- **User Management**: CRUD operations for users
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
│   ├── models/
│   │   └── user.model.ts
│   ├── services/
│   │   ├── auth.service.ts
│   │   └── user.service.ts
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

- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID (with Redis caching)
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

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
2. **View Users**: After login, you'll see the user management dashboard
3. **Add User**: Click "Add User" button to create a new user
4. **Edit User**: Click "Edit" button next to any user to modify their details
5. **Delete User**: Click "Delete" button to remove a user (with confirmation)
6. **Logout**: Click "Logout" to end your session

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
