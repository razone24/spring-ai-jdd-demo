-- Runs once, on the first start of an empty data volume, as the `jdd` superuser.
-- Extensions the pgvector stores need, created once here so the two vector-store servers don't race
-- to create them concurrently on their first start.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Read-only role for Grafana's Postgres datasource (audit tables, chat memory, vector tables).
CREATE ROLE grafana LOGIN PASSWORD 'grafana';
GRANT CONNECT ON DATABASE jdd TO grafana;
GRANT USAGE ON SCHEMA public TO grafana;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO grafana;
ALTER DEFAULT PRIVILEGES FOR ROLE jdd IN SCHEMA public GRANT SELECT ON TABLES TO grafana;
