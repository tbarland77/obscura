# Docker Compose Guide

This project includes three Docker Compose configurations for different development scenarios.

## Quick Reference

### H2 Development (Fastest)
```bash
docker-compose up
```
- Uses H2 in-memory database
- Live code reload with Spring DevTools
- No data persistence (DB resets on restart)
- Best for: Rapid feature development

### PostgreSQL Development (Production-like with Hot Reload)
```bash
docker-compose -f docker-compose.postgres-dev.yml up
```
- Uses PostgreSQL 17 (matches production)
- Live code reload with Spring DevTools
- Data persisted in Docker volume
- Best for: Testing Flyway migrations during development

### PostgreSQL Production-like (Final Testing)
```bash
docker-compose -f docker-compose.postgres.yml up
```
- Uses PostgreSQL 17 (matches production)
- Production-like build (no hot reload)
- Data persisted in Docker volume
- Best for: Final verification before raising PR

## Common Operations

### Start Services
```bash
# H2 development
docker-compose up

# PostgreSQL development (detached mode)
docker-compose -f docker-compose.postgres-dev.yml up -d

# PostgreSQL production-like
docker-compose -f docker-compose.postgres.yml up
```

### Stop Services
```bash
# H2
docker-compose down

# PostgreSQL (keep data)
docker-compose -f docker-compose.postgres.yml down

# PostgreSQL (remove data - fresh start)
docker-compose -f docker-compose.postgres.yml down -v
```

### View Logs
```bash
# H2
docker-compose logs -f app

# PostgreSQL
docker-compose -f docker-compose.postgres-dev.yml logs -f app
docker-compose -f docker-compose.postgres-dev.yml logs -f postgres
```

### Access PostgreSQL Database
```bash
# When PostgreSQL compose is running
docker exec -it obscura-postgres-dev psql -U obscura -d obscura

# Or use any PostgreSQL client
# Host: localhost
# Port: 5432
# Database: obscura
# Username: obscura
# Password: local_test_password
```

## Workflow Recommendations

### Feature Development
1. Start with H2 for fastest iteration:
   ```bash
   docker-compose up
   ```

2. Test changes frequently with unit tests:
   ```bash
   ./gradlew test --tests StoryServiceTests
   ```

### Database Migration Development
1. Use PostgreSQL development mode:
   ```bash
   docker-compose -f docker-compose.postgres-dev.yml up
   ```

2. Create migration in `src/main/resources/db/migration/`
3. Restart app to see migration applied
4. Test with Flyway integration tests:
   ```bash
   ./gradlew test --tests FlywayMigrationTests
   ```

### Before Raising PR
1. Run PostgreSQL production-like build:
   ```bash
   docker-compose -f docker-compose.postgres.yml up
   ```

2. Run full test suite (if Docker available):
   ```bash
   ./gradlew clean build
   ```

3. Verify all endpoints work with production database

## Troubleshooting

### Port Already in Use
```bash
# Stop existing containers
docker-compose down
docker-compose -f docker-compose.postgres.yml down

# Check what's using port 8080 or 5432
netstat -ano | findstr :8080  # Windows
lsof -i :8080                  # macOS/Linux
```

### Database Migration Issues
```bash
# Reset PostgreSQL database completely
docker-compose -f docker-compose.postgres-dev.yml down -v
docker-compose -f docker-compose.postgres-dev.yml up

# This removes all data and reruns migrations from scratch
```

### App Can't Connect to Database
- Ensure PostgreSQL health check passes (wait ~10 seconds on first start)
- Check logs: `docker-compose -f docker-compose.postgres.yml logs postgres`
- Verify environment variables are set correctly

### Performance Issues
```bash
# If app is slow, check resource usage
docker stats

# Allocate more memory to Docker Desktop if needed
# Settings > Resources > Memory (recommend 4GB+)
```

## Data Persistence

### H2
- **No persistence** - Data is lost when container stops
- Database recreated on each startup

### PostgreSQL
- **Persistent** - Data stored in Docker volumes:
  - `postgres-data` (production-like)
  - `postgres-dev-data` (development)

To reset database:
```bash
docker-compose -f docker-compose.postgres.yml down -v
```

To backup database:
```bash
docker exec obscura-postgres pg_dump -U obscura obscura > backup.sql
```

To restore database:
```bash
docker exec -i obscura-postgres psql -U obscura obscura < backup.sql
```
