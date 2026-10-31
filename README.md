# OutfitRating

A community rating platform for clothing. Members publish complete outfits or individual garments,
and everyone else scores them across five criteria — **style, fit, colour, originality, quality** —
which roll up into a single 0–100 verdict. Those verdicts drive weekly and monthly leaderboards,
personal wardrobes, and a recommendation feed built from each member's own taste profile.

The interaction model is modelled on [risazatvorchestvo.com](https://risazatvorchestvo.com), a
multi-criteria music rating site, reworked for fashion.

| | |
|---|---|
| **Backend** | Java 17 · Spring Boot 4.1 · Spring Security 7 (JWT) · Spring Data JPA / Hibernate 7 · Flyway · PostgreSQL 17 · springdoc OpenAPI |
| **Frontend** | TypeScript · React 19 · Vite 8 · TanStack Query 5 · React Router 7 · Tailwind CSS 4 |
| **Infra** | Docker Compose · Gradle 9 · JUnit 5 / AssertJ |

---

## Running it

### Everything, one command (Windows)

```powershell
powershell -ExecutionPolicy Bypass -File start.ps1
```

Starts Docker Desktop if it is not already running — it does not auto-start with Windows, which is
the usual reason `docker compose up` fails after a reboot — then brings the stack up and waits for
the API to pass its health check before printing the URLs.

### 1. Database

```bash
docker compose up -d db
```

Postgres is published on **5433** by default (not 5432) so it never collides with a PostgreSQL
already installed on the machine. Override with `DB_PORT` in `.env`.

### 2. Backend

```bash
cd backend/OutfitRating && ./gradlew bootRun
```

Needs a JDK 17+ on `JAVA_HOME`. On first start Flyway creates the schema and a seeder fills an
empty database with 50 looks, 10 members and ~290 verdicts, so the app is never a blank page.
Every demo account uses the password `password123` — sign in as `mara` (admin), `liv` (curator) or
any of `kenji`, `theo`, `nadia`, `sasha`, `ines`, `darius`, `yuki`, `amina`.

- API: <http://localhost:8080/api>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

Set `APP_SEED_DEMO=false` to start with an empty catalogue.

### 3. Frontend

```bash
cd frontend && npm install && npm run dev
```

<http://localhost:5173>. The Vite dev server proxies `/api` and `/uploads` to port 8080, so the
browser only ever talks to one origin — no CORS in the development loop, and the HttpOnly refresh
cookie behaves exactly as it will behind a production reverse proxy.

### Everything at once

```bash
docker compose --profile full up --build
```

Builds both images and serves the SPA from nginx on <http://localhost:5173>, with `/api` proxied to
the Spring container.

---

## How the scoring works

Each verdict carries five criteria scored 1–10. They are composed into a 0–100 number with **equal
weights** — weighting style above fit would quietly encode an editorial opinion into what is meant
to be a community score. The scale is stretched to 100 because a two-digit number reads as a
verdict, while "7.4/10" reads as a measurement.

**One verdict per person per look.** Rating a look you have already rated replaces your previous
score rather than adding a second one, and you cannot rate a look you submitted yourself.

Averages are stored denormalised on the `looks` row (`avg_score` plus one column per criterion) and
recomputed in a single aggregate query after every write. Catalogue queries and leaderboards then
never need to group over the whole review table.

**Leaderboards** rank by the mean of the verdicts written *inside* the selected window, not by the
all-time average — otherwise a look that peaked six months ago would squat on the weekly chart
forever. A minimum verdict count per period keeps a single enthusiastic 100 out of the top spot.

**Recommendations** are content-based: the taste profile is the set of tags on the looks a member
scored 70 or higher; candidates are unseen looks sharing those tags, ranked on tag overlap first and
community score second. Every suggestion carries the reason it was picked. Collaborative filtering
would need a user base this project does not have; tag overlap degrades gracefully to "the
best-rated things you have not seen".

---

## API

Reads are public by design — the catalogue, leaderboards and profiles are the product's shop
window. Anything that writes needs a bearer token.

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/auth/register` · `/login` · `/refresh` · `/logout` | Access token in the body, refresh token in an HttpOnly cookie |
| `GET` `PATCH` | `/api/auth/me` | Current member and profile edits |
| `GET` | `/api/looks` | Filter by `q`, `type`, `category`, `season`, `occasion`, `brand`, `tag`, `author`, `minScore`; sort and paginate |
| `GET` | `/api/looks/{slug}` · `/similar` · `/reviews` · `/comments` | Detail, related looks, verdicts, discussion |
| `POST` `PUT` `DELETE` | `/api/looks` · `/api/looks/{slug}` | Publish, edit and withdraw (author or admin) |
| `PUT` | `/api/looks/{slug}/review` | Create-or-replace your verdict |
| `POST` | `/api/looks/{slug}/favorite` · `/comments` | Wardrobe and discussion |
| `POST` `DELETE` | `/api/reviews/{id}/like` · `/api/reviews/{id}` | Helpful votes, deletion |
| `GET` | `/api/charts/{WEEK\|MONTH\|YEAR\|ALL_TIME}` · `/api/charts/trending/now` | Leaderboards |
| `GET` | `/api/recommendations` | Personalised, authenticated |
| `GET` | `/api/users/{username}` · `/looks` · `/reviews` · `/wardrobe` · `/followers` · `/following` | Profiles |
| `POST` `DELETE` | `/api/users/{username}/follow` | Social graph |
| `POST` | `/api/uploads` | Multipart image upload |
| `GET` | `/api/brands` · `/api/tags` · `/api/stats` · `/api/taxonomy` | Reference data |

Full schemas live in Swagger UI.

---

## Security

- **Access tokens** are short-lived JWTs (HS384, 60 min) held in memory by the SPA. Putting them in
  `localStorage` would turn any XSS bug into a permanent account takeover.
- **Refresh tokens** are opaque random values sent only as an HttpOnly cookie. Only their SHA-256
  digest is stored, so a database leak hands out no usable sessions, and every refresh **rotates**
  the token — a stolen one is usable at most once before the legitimate client invalidates it.
- Passwords are BCrypt-hashed. Sign-in accepts either the username or the e-mail.
- Roles: `USER`, `CURATOR`, `ADMIN`. Ownership checks guard every edit and delete.
- Uploads are restricted by content type and size, and resolved paths are verified to stay inside
  the storage root.

`app.jwt.secret` ships with an obvious development default and the application refuses to start if
the configured secret is shorter than 32 bytes. Set `APP_JWT_SECRET` in any real deployment, along
with `APP_SECURE_COOKIE=true`.

---

## Layout

```
backend/OutfitRating/
  src/main/java/com/outfitrating/
    config/      Security, CORS, OpenAPI, static uploads, demo seeder
    domain/      JPA entities and enums
    repo/        Spring Data repositories, aggregate + ranking queries
    dto/         Request and response records
    service/     Business logic: scoring, catalogue, reviews, charts, recommendations
    web/         REST controllers and the global exception handler
  src/main/resources/db/migration/   Flyway migrations
  src/test/                          Unit, repository and end-to-end tests

frontend/src/
  lib/          API client, auth context, typed queries, formatting
  components/   Layout, UI primitives, look/review/comment components
  pages/        Home, Catalogue, Look, Charts, For you, Submit, Profile, Settings, Auth
```

## Tests

```bash
cd backend/OutfitRating && ./gradlew test
```

27 tests: score composition and slug generation as unit tests, the aggregate query against a real
database, and an end-to-end pass over register → publish → rate → re-rate that also covers the
authorization rules and the error envelope.

Tests run on in-memory H2 in PostgreSQL mode with Hibernate generating the schema from the same
entity mappings. The Flyway migrations are PostgreSQL dialect and are verified against the real
container on every normal run, where `ddl-auto: validate` proves the entities and the migrations
still agree.

## Notes

- Demo photography comes from Unsplash contributors and is referenced by URL; uploaded images are
  written to `backend/OutfitRating/uploads` through a `StorageService` interface whose only
  implementation is disk-backed — swapping in S3 is a one-class change.
- Avatars in the seed data come from pravatar.cc.
