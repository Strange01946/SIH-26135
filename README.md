# SIH 26135

Skill-training to employment outcome tracking for government skill programmes (Smart India Hackathon problem 26135).

## Layout

| Path | Role |
| --- | --- |
| `database/` | MySQL 8 schema, views, and validation SQL (Phases 00–19). Final for now. |
| `backend/` | Spring Boot (Maven) API. Persistence connection in Phase 20.2; no business APIs yet. |
| `frontend/` | Placeholder for a later client application. Not initialized yet. |
| `docs/` | Short project notes. |

## Status

- **Database design is complete** (144 base tables, 21 analytical views, no demo/seed data).
- **Backend persistence foundation is configured** (Phase 20.2: JPA/Hibernate `ddl-auto=validate` against MySQL `SIH26135`; no business entities yet). Requires `DB_USERNAME` and `DB_PASSWORD`.

## Backend

Requires Java 17, Maven, and a running MySQL 8 instance with database `SIH26135`.

Set credentials in the environment (do not commit them):

| Variable | Required | Default |
| --- | --- | --- |
| `DB_USERNAME` | yes | — |
| `DB_PASSWORD` | yes | — |
| `DB_HOST` | no | `localhost` |
| `DB_PORT` | no | `3306` |
| `DB_NAME` | no | `SIH26135` |

```bash
cd backend
mvn test
mvn spring-boot:run
```

Hibernate is configured not to create or alter schema. Later phases will add entities, APIs, and security module by module.
