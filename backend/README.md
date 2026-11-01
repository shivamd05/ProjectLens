# ProjectLens Spring Boot API

This directory contains the Java 21 / Spring Boot backend. Run commands from this directory so the local `.env`, legacy JSON path, and upload paths resolve as configured.

## Setup

1. Create a MySQL database named `projectlense`.
2. Copy `.env.example` to `.env` and configure `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and a long private `PROJECTLENSE_ADMIN_TOKEN`. Add `OPENAI_API_KEY` for similarity checks. Students register at `/api/auth/register`; the configured admin token is used for admin sign-in and faculty provisioning.
3. Start the API:

```bash
mvn spring-boot:run
```

Flyway applies migrations from `src/main/resources/db/migration/`; Hibernate validates the schema. Run tests with `mvn test`.

See the repository root README for frontend setup, API routes, and the full-stack run instructions.
