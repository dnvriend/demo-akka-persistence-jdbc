CREATE SCHEMA IF NOT EXISTS akka_persistence_jdbc;

DROP TABLE IF EXISTS akka_persistence_jdbc.journal;

CREATE TABLE IF NOT EXISTS akka_persistence_jdbc.journal (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  created BIGINT NOT NULL,
  tags VARCHAR(255) DEFAULT NULL,
  message BYTEA NOT NULL,
  PRIMARY KEY(persistence_id, sequence_number)
);

DROP TABLE IF EXISTS akka_persistence_jdbc.deleted_to;

CREATE TABLE IF NOT EXISTS akka_persistence_jdbc.deleted_to (
  persistence_id VARCHAR(255) NOT NULL,
  deleted_to BIGINT NOT NULL
);

DROP TABLE IF EXISTS akka_persistence_jdbc.snapshot;

CREATE TABLE IF NOT EXISTS akka_persistence_jdbc.snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  created BIGINT NOT NULL,
  snapshot BYTEA NOT NULL,
  PRIMARY KEY(persistence_id, sequence_number)
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
