# ProjectLens Spring Boot API

This directory contains the Java 21 / Spring Boot backend. Run commands from this directory so the local `.env`, legacy JSON path, and upload paths resolve as configured.

## Setup

1. Create a MySQL database named `projectlens` (the `.env.example` connection URL points to this database).
2. Copy `.env.example` to `.env` and configure `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and a long private `PROJECTLENSE_ADMIN_TOKEN`. Add `OPENAI_API_KEY` for similarity checks. Students register at `/api/auth/register`; the configured admin token is used for admin sign-in and faculty provisioning.
3. Start the API:

```bash
mvn spring-boot:run
```

Flyway applies migrations from `src/main/resources/db/migration/`; Hibernate validates the schema. Run tests with `mvn test`.

See the repository root README for frontend setup, API routes, and the full-stack run instructions.

## Rubric and final evaluation workflow

Flyway V4 adds the rubric, progress-review, submission, and AI-evaluation tables without changing the existing project or authentication tables. It seeds active PDF and ZIP version 1 rubrics (each totaling 100) and the configurable 20/30/50 progress review limits.

Configure at least one provider key in `.env`; the application tries Gemini first and then OpenAI when both are configured:

```dotenv
GEMINI_API_KEY=your-gemini-api-key
GEMINI_MODEL=gemini-3.8-flash
OPENAI_API_KEY=your-openai-api-key
OPENAI_MODEL=gpt-6-luna
```

Before students submit, an administrator must provision faculty accounts. Students then select a guide from `GET /api/faculty-guides`, create the project with `POST /api/projects`, and upload a `.pdf` or `.zip` file to `POST /api/projects/{projectId}/submissions` using the multipart field `file`. The upload limit is 100 MB.

Faculty enter one record for each configured stage using `GET /api/projects/{projectId}/reviews` and `POST /api/projects/{projectId}/reviews`. Only after all three reviews and a final submission exist can the assigned faculty guide call `POST /api/projects/{projectId}/evaluate`. The returned AI score is a recommendation; the faculty member must call `POST /api/evaluations/{evaluationId}/approve` or `/reject`. Progress review marks are never added to the final rubric score. Students can see the final evaluation only after it is approved.

Admins manage rubric versions at `/api/rubrics` and can change progress review limits with `PUT /api/admin/review-stages/{id}`. A rubric version already used by an evaluation cannot have its criteria changed; create the next version instead. The admin portal exposes both controls.

Run `mvn test` for the H2-backed integration suite. It exercises PDF and ZIP uploads, review gating, ZIP/PDF rubric selection, invalid marks, malformed LLM output, access checks, faculty approval, and rubric version immutability. Use a real configured provider key to run an end-to-end AI evaluation against a sample submission.
