Production Setup:

Dockerfile - Multi-stage build with Nginx
compose.yaml - Full production stack
nginx.conf - Optimized Nginx configuration
.dockerignore - Exclude unnecessary files

Development Setup:

Dockerfile.dev - Development container with hot reload
compose.dev.yaml - Development environment
database/init.sql - Database schema and sample data

Utilities:

Makefile - Easy commands for Docker operations

Quick Start:
Production:

# Build and start production environment
make prod-up

Development:

# Start development environment with hot reload
make dev-up

🌐 Access URLs:

Frontend: http://localhost:4200
Backend API: http://localhost:8080
Database: localhost:5432 (prod) / localhost:5433 (dev)
Redis: localhost:6379 (prod) / localhost:6380 (dev)

✨ Key Features:
Production Container:

Multi-stage build (build + nginx)
Optimized for security (non-root user)
Gzip compression enabled
Health checks included
Static asset caching
API proxy configuration

Development Container:

Hot reload support
Debug ports exposed
Volume mounting for live code changes
Separate databases to avoid conflicts

Infrastructure:

PostgreSQL database with sample data
Redis caching layer
Nginx reverse proxy (optional)
Network isolation between services

📋 Available Commands:

make help          # Show all available commands
make build         # Build Docker images
make up            # Start production
make dev-up        # Start development
make down          # Stop all services
make logs          # View logs
make clean         # Clean up everything

🔧 Customization:

Modify database/init.sql for your database schema
Update nginx.conf for custom routing
Adjust environment variables in compose files
Replace backend placeholder with your actual Spring Boot app

The setup is production-ready with security best practices, health checks, and optimized performance!
