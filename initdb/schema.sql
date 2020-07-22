-- default schema
DROP TABLE IF EXISTS public.journal;

CREATE TABLE IF NOT EXISTS public.journal
(
    ordering        BIGSERIAL,
    sequence_number BIGINT                NOT NULL,
    deleted         BOOLEAN DEFAULT FALSE NOT NULL,
    persistence_id  TEXT                  NOT NULL,
    message         BYTEA                 NOT NULL,
    tags            int[],
    PRIMARY KEY (persistence_id, sequence_number)
);

CREATE EXTENSION IF NOT EXISTS intarray WITH SCHEMA public;
CREATE INDEX journal_tags_idx ON public.journal USING GIN (tags gin__int_ops);
CREATE INDEX journal_ordering_idx ON public.journal USING BRIN (ordering);

DROP TABLE IF EXISTS public.tags;

CREATE TABLE IF NOT EXISTS public.tags
(
    id              BIGSERIAL,
    name            TEXT                        NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS tags_name_idx on public.tags (name);

DROP TABLE IF EXISTS public.snapshot;

CREATE TABLE IF NOT EXISTS public.snapshot
(
    persistence_id  TEXT   NOT NULL,
    sequence_number BIGINT NOT NULL,
    created         BIGINT NOT NULL,
    snapshot        BYTEA  NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);

-- akka-persistence-postgres partitioned schema
CREATE SCHEMA IF NOT EXISTS akka_persistence_postgres;

DROP TABLE IF EXISTS akka_persistence_postgres.journal;

CREATE TABLE IF NOT EXISTS akka_persistence_postgres.journal
(
    ordering        BIGSERIAL,
    sequence_number BIGINT                NOT NULL,
    deleted         BOOLEAN DEFAULT FALSE NOT NULL,
    persistence_id  TEXT                  NOT NULL,
    message         BYTEA                 NOT NULL,
    tags            int[],
    PRIMARY KEY (persistence_id, sequence_number)
) PARTITION BY LIST (persistence_id);

CREATE EXTENSION IF NOT EXISTS intarray WITH SCHEMA akka_persistence_postgres;
CREATE INDEX journal_tags_idx ON akka_persistence_postgres.journal USING GIN (tags gin__int_ops);
CREATE INDEX journal_ordering_idx ON akka_persistence_postgres.journal USING BRIN (ordering);

DROP TABLE IF EXISTS akka_persistence_postgres.tags;

CREATE TABLE IF NOT EXISTS akka_persistence_postgres.tags
(
    id   BIGSERIAL,
    name TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS tags_name_idx on akka_persistence_postgres.tags (name);

DROP TABLE IF EXISTS akka_persistence_postgres.snapshot;

CREATE TABLE IF NOT EXISTS akka_persistence_postgres.snapshot
(
    persistence_id  TEXT   NOT NULL,
    sequence_number BIGINT NOT NULL,
    created         BIGINT NOT NULL,
    snapshot        BYTEA  NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);


-- person schema
CREATE SCHEMA IF NOT EXISTS person;

DROP TABLE IF EXISTS person.persons;

CREATE TABLE IF NOT EXISTS person.persons (
  id VARCHAR(255) NOT NULL,
  firstname VARCHAR(255) NOT NULL,
  lastname VARCHAR(255) NOT NULL,
  updated BIGINT NOT NULL,
  primary key(id)
);

-- counter schema
CREATE SCHEMA IF NOT EXISTS counter;

DROP TABLE IF EXISTS counter.event_log;

CREATE TABLE IF NOT EXISTS counter.event_log (
  persistence_id VARCHAR(255) NOT NULL, -- identifies the actor
  sequence_number BIGINT NOT NULL, -- identifies the sequence of events
  tags VARCHAR(255) DEFAULT NULL, -- arbitrary tags that can be set on the event
  event_type VARCHAR(255) NOT NULL, -- event discriminator
  created BIGINT NOT NULL, -- timestamp
  primary key(persistence_id, sequence_number) -- primary key
);

DROP TABLE IF EXISTS counter.event_log_deleted_to;

CREATE TABLE IF NOT EXISTS counter.event_log_deleted_to (
  persistence_id VARCHAR(255) NOT NULL,
  deleted_to BIGINT NOT NULL
);

-- counter incremented event typed information
DROP TABLE IF EXISTS counter.incremented;

-- counter incremented event typed information
CREATE TABLE IF NOT EXISTS counter.incremented (
  persistence_id VARCHAR(255) NOT NULL, -- identifies the actor
  sequence_number BIGINT NOT NULL, -- identifies the sequence of events
  incremented_by INT NOT NULL -- typed information
);

-- fk constraint
ALTER TABLE counter.incremented add constraint incr_el_fk foreign key(persistence_id, sequence_number) references counter.event_log(persistence_id, sequence_number) on update NO ACTION on delete NO ACTION;

-- counter decremented event typed information
DROP TABLE IF EXISTS counter.decremented;

-- counter decremented event typed information
CREATE TABLE IF NOT EXISTS counter.decremented (
  persistence_id VARCHAR(255) NOT NULL, -- identifies the actor
  sequence_number BIGINT NOT NULL, -- identifies the sequence of events
  decremented_by INT NOT NULL -- typed information
);

-- fk constraint
ALTER TABLE counter.decremented add constraint decr_el_fk foreign key(persistence_id, sequence_number) references counter.event_log(persistence_id, sequence_number) on update NO ACTION on delete NO ACTION;

DROP TABLE IF EXISTS counter.snapshot;

CREATE TABLE IF NOT EXISTS counter.snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  created BIGINT NOT NULL,
  snapshot BYTEA NOT NULL,
  PRIMARY KEY(persistence_id, sequence_number)
);
