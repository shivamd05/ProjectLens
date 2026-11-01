# Database

Create the MySQL database with:

```sql
CREATE DATABASE projectlense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

The canonical schema migrations are the Flyway scripts in `backend/src/main/resources/db/migration/`. They stay with the Spring Boot application because Flyway loads them from the backend classpath at startup. Keep future schema changes as new versioned migrations in that directory; do not edit migrations that have already been applied to a database.

No separate `schema.sql` is maintained, to avoid creating a second schema source of truth. Connection settings belong in the ignored `backend/.env` file, based on `backend/.env.example`.
