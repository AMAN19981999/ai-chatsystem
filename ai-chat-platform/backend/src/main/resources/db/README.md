Database files live here.

- `supabase_schema.sql`: starter PostgreSQL schema for Supabase SQL Editor.
- `add_username_to_app_users.sql`: small upgrade script if `app_users` already exists.

Later, move this schema into versioned Flyway or Liquibase migrations before production rollout.
