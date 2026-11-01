# ProjectLens

ProjectLens is an academic project evaluation and feedback application. It provides a React interface for students, faculty, and administrators, with a Spring Boot REST API, MySQL persistence, Excel registration import, and semantic project similarity checks.

## Features

- PDF and ZIP project submission and rubric evaluation.
- Student, faculty, and administrator dashboards.
- Faculty review and approval workflows.
- MySQL-backed project, member, import, embedding, and similarity data.
- Admin Excel import with preview, duplicate warnings, correction fields, approve, and cancel actions.
- OpenAI embedding-based project similarity checks.

Student and faculty sessions are issued by the Spring API and stored as hashed, revocable tokens in MySQL. Students can register through the frontend; administrators sign in with `PROJECTLENSE_ADMIN_TOKEN` and provision faculty accounts. Demo credentials, seeded browser projects, and canned AI chat responses have been removed.

## Technology

- Frontend: React 18, Vite, JavaScript, Tailwind CSS.
- Backend: Java 21, Spring Boot 3, Spring Data JPA, Hibernate, Spring Security, Maven.
- Database: MySQL with Flyway migrations.

## Repository layout

```text
ProjectLens/
├── frontend/                 # React/Vite application
│   ├── src/
│   ├── index.html
│   ├── hero.html
│   ├── package.json
│   └── vite.config.js
├── backend/                  # Spring Boot application
│   ├── src/main/java/
│   ├── src/main/resources/db/migration/
│   ├── pom.xml
│   └── README.md
├── database/                 # Database setup notes; Flyway SQL stays with backend
├── documentation/            # Project documents and diagrams
├── .gitignore
└── README.md
```

Existing `src` content and frontend configuration were moved intact under `frontend/`. The Java package layout and Flyway migration locations remain intact under `backend/`. Build output and dependency folders are ignored by Git.

## Prerequisites

- Node.js 18 or newer and npm.
- JDK 21 and Maven (the project does not include Maven Wrapper).
- MySQL 8 or newer.
- An OpenAI API key only if you want similarity checks and embedding generation.

## MySQL setup

Create the database once:

```sql
CREATE DATABASE projectlense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Copy `backend/.env.example` to `backend/.env` and set `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and a private `PROJECTLENSE_ADMIN_TOKEN`. Set `OPENAI_API_KEY` to enable semantic similarity. Never commit `backend/.env` or real credentials. Flyway runs the checked-in migrations in `backend/src/main/resources/db/migration/` at backend startup; Hibernate validates the resulting schema.

The frontend optionally reads `VITE_API_BASE_URL` from `frontend/.env.local`. Its default API URL is `http://127.0.0.1:8000`.

## Run locally

Start MySQL, then run the backend in one terminal:

```bash
cd backend
mvn spring-boot:run
```

Run the frontend in a second terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open the local URL printed by Vite (normally `http://localhost:5173`). To build the frontend for production:

```bash
cd frontend
npm run build
```

Run backend tests with:

```bash
cd backend
mvn test
```

## API integration

The React API client in `frontend/src/services/api.js` uses `VITE_API_BASE_URL` or defaults to `http://127.0.0.1:8000`.

Existing project routes:

- `GET /`
- `POST /api/projects/upload` (multipart PDF/ZIP submission)
- `GET /api/projects` and `GET /api/projects/{project_id}`
- `POST /api/projects/{project_id}/faculty-review`
- `POST /api/projects/{project_id}/approve`

Excel administration routes use `X-Admin-Token` matching `PROJECTLENSE_ADMIN_TOKEN`:

- `POST /api/v1/admin/excel-imports` (multipart field `file`)
- `GET /api/v1/admin/excel-imports/{jobId}`
- `GET /api/v1/admin/excel-imports/{jobId}/preview`
- `POST /api/v1/admin/excel-imports/{jobId}/approve`
- `POST /api/v1/admin/excel-imports/{jobId}/cancel`

Similarity routes:

- `POST /api/v1/projects/similarity-checks`
- `GET /api/v1/projects/similarity-checks/{checkId}`

Excel upload creates a reviewable preview; valid rows are inserted into the projects tables only after approval. Embeddings are indexed asynchronously. A configured OpenAI key is required for similarity checks.

## Database scripts

`database/README.md` documents the migration source of truth. The Flyway scripts remain in the backend resources so they are packaged and applied automatically by Spring Boot. No duplicate `schema.sql` is maintained.

## License

No license is included yet. Choose the appropriate license and rights holder before publishing this repository publicly.
