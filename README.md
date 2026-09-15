# SIH 26135

Skill-training to employment outcome tracking for government skill programmes (Smart India Hackathon problem 26135).

## Layout

| Path | Role |
| --- | --- |
| `database/` | MySQL 8 schema, views, and validation SQL (Phases 00–19). Final for now. |
| `backend/` | Spring Boot (Maven) API. Bootstrap only in Phase 20.1. |
| `frontend/` | Placeholder for a later client application. Not initialized yet. |
| `docs/` | Short project notes. |

## Status

- **Database design is complete** (144 base tables, 21 analytical views, no demo/seed data).
- **Backend development is beginning** (Phase 20.1: compile-and-start Spring Boot, no MySQL connection yet).

## Backend (Phase 20.1)

```bash
cd backend
mvn test
mvn spring-boot:run
```

Requires Java 17 and Maven. Later phases will add persistence, APIs, and security module by module.
