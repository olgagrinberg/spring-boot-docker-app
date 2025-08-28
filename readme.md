# Spring Boot Docker Compose Application

This is a complete Spring Boot application configured with Docker Compose integration using the `spring-boot-docker-compose` dependency. The application includes PostgreSQL database, Redis cache, and provides RESTful APIs for user management.

## Features , use mariadb 

- **Spring Boot 3.2.0** with Java 17
- **Docker Compose Integration** - Automatically manages Docker containers
- **mariadb Database** - For persistent data storage
- **Redis Cache** - For caching functionality
- **RESTful APIs** - CRUD operations for User entity
- **Spring Data JPA** - Database operations
- **Spring Boot Actuator** - Health checks and metrics

## Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Maven 3.6+

## Project Structure

```
spring-boot-docker-app/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/
│       │       ├── SpringBootDockerApplication.java
│       │       ├── config/
│       │       │   └── RedisConfig.java
│       │       ├── controller/
│       │       │   └── UserController.java
│       │       ├── entity/
│       │       │   └── User.java
│       │       └── repository/
│       │           └── UserRepository.java
│       └── resources/
│           └── application.properties
├── compose.yaml
├── Dockerfile
├── pom.xml
└── README.md
```

## How It Works

The `spring-boot-docker-compose` dependency automatically:

1. **Detects** the `compose.yaml` file in your project root
2. **Starts** required Docker containers when the application starts
3. **Configures** connection properties automatically
4. **Stops** containers when the application shuts down

## Running the Application

### Method 1: Using Maven (Recommended)

1. Clone the repository
2. Navigate to the project directory
3. Run the application:

```bash
./mvnw spring-boot:run
```

The application will automatically:
- Start PostgreSQL and Redis containers
- Configure database connections
- Start the Spring Boot application on port 8080

### Method 2: Using Docker

1. Build and run using Docker Compose:

```bash
docker-compose up --build
```

## Configuration Details

### Docker Compose Configuration

The `compose.yaml` file defines:

- **mariadb** on port 5432
- **Redis 7** on port 6379
- **Health checks** for both services
- **Persistent volumes** for data storage

### Spring Boot Configuration

Key properties in `application.properties`:

## API Endpoints

The application provides the following REST endpoints:

### User Management

- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID (with Redis caching)
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Health Check

- `GET /api/users/health` - Application health status
- `GET /actuator/health` - Detailed health information

### Example API Usage

**Create a user:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com"}'
```

**Get all users:**
```bash
curl http://localhost:8080/api/users
```

**Get user by ID:**
```bash
curl http://localhost:8080/api/users/1
```

## Docker Compose Lifecycle

The application manages Docker containers automatically:

- **Startup**: Containers start when Spring Boot application starts
- **Development**: Hot reload works with running containers
- **Shutdown**: Containers stop when application stops
- **Cleanup**: Use `docker-compose down -v` to remove volumes

## Development Features

- **Auto-restart** with Spring Boot DevTools
- **SQL logging** enabled for development
- **Actuator endpoints** for monitoring
- **Redis caching** with automatic serialization

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 5432, 6379, and 8080 are available
2. **Docker issues**: Make sure Docker is running
3. **Database connection**: Check Docker container logs

### Viewing Logs

```bash
# Application logs
./mvnw spring-boot:run

# Docker container logs
docker-compose logs mariadb
docker-compose logs redis
```

### Stopping Services

```bash
# Stop application (Ctrl+C)
# Or stop Docker containers manually
docker-compose down
```

## Production Deployment

For production deployment:

1. Update `application-prod.properties` with production settings
2. Use environment variables for sensitive data
3. Configure proper logging
4. Set up monitoring and metrics
5. Use Docker secrets for passwords

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.