# db_scripts

`query_log.txt` is a single, running, dated log of every manual SQL change
made to this project's database - newest entries appended at the bottom.

## Why this exists

The project relies on `spring.jpa.hibernate.ddl-auto=update` to auto-add new
columns/tables on startup. That works for a brand new table, but it
**silently fails** for a `NOT NULL` column added to a table that already has
rows (MySQL has no value to backfill existing rows with - the ALTER is just
skipped, and the app starts up anyway with the column missing). The same is
true for anything else Hibernate can't safely automate: dropped columns,
renamed columns, data backfills. Those changes get written here instead and
run by hand.

## How to run

```bash
mysql -u <user> -p <database> < query_log.txt
```

Statements already applied (e.g. by `ddl-auto=update` on an empty dev DB)
will error with something like "Duplicate column name" - that's harmless,
it just means that specific change is already in place. Keep going.

## Adding a new entry

When a future change needs a manual script (a new `NOT NULL` column on a
populated table, a drop, a backfill, etc.), append a new dated section to the
bottom of `query_log.txt` in the same format as the existing ones - don't
edit or remove past entries; they're the record of what's already been run
against which environments.

## Longer term

This file is a stopgap. As the number of manual entries grows, moving to
Flyway (or Liquibase) would turn "run this file by hand" into something the
app applies automatically and tracks itself (in a `flyway_schema_history`
table), so this never has to be a manual step again. The entries in
`query_log.txt` would become the first migrations if you want to do that.
