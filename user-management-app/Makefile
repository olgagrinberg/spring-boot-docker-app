# User Management Angular App - Docker Commands

.PHONY: help build up down logs clean dev-up dev-down prod-up prod-down

# Default target
help:
	@echo "Available commands:"
	@echo "  build      - Build Docker images"
	@echo "  up         - Start all services (production)"
	@echo "  down       - Stop all services"
	@echo "  logs       - View logs for all services"
	@echo "  clean      - Remove containers, networks, and images"
	@echo "  dev-up     - Start development environment"
	@echo "  dev-down   - Stop development environment"
	@echo "  prod-up    - Start production environment"
	@echo "  prod-down  - Stop production environment"
	@echo "  restart    - Restart all services"
	@echo "  status     - Show service status"

# Build images
build:
	docker-compose -f compose.yaml build

# Production environment
prod-up:
	docker-compose -f compose.yaml up -d
	@echo "Production environment started!"
	@echo "Frontend: http://localhost:4200"
	@echo "Backend API: http://localhost:8080"

prod-down:
	docker-compose -f compose.yaml down

# Development environment
dev-up:
	docker-compose -f compose.dev.yaml up -d
	@echo "Development environment started!"
	@echo "Frontend: http://localhost:4200 (with hot reload)"
	@echo "Backend API: http://localhost:8080"
	@echo "Database: localhost:5433"
	@echo "Redis: localhost:6380"

dev-down:
	docker-compose -f compose.dev.yaml down

# Default up command (production)
up: prod-up

# Stop all services
down:
	docker-compose -f compose.yaml down
	docker-compose -f compose.dev.yaml down

# View logs
logs:
	docker-compose -f compose.yaml logs -f

dev-logs:
	docker-compose -f compose.dev.yaml logs -f

# Restart services
restart:
	make down
	make up

# Show service status
status:
	docker-compose -f compose.yaml ps

dev-status:
	docker-compose -f compose.dev.yaml ps

# Clean up everything
clean:
	docker-compose -f compose.yaml down -v --rmi all --remove-orphans
	docker-compose -f compose.dev.yaml down -v --rmi all --remove-orphans
	docker system prune -af

# Database operations
db-migrate:
	docker-compose -f compose.yaml exec database psql -U userapp -d userdb -f /docker-entrypoint-initdb.d/init.sql

# Install dependencies and build locally
install:
	npm install

build-local:
	ng build --configuration production

# Run tests
test:
	ng test --browsers=ChromeHeadless --watch=false

# Lint code
lint:
	ng lint
