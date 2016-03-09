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

DROP TABLE IF EXISTS counter_event_log;

CREATE TABLE IF NOT EXISTS counter_event_log (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  created BIGINT NOT NULL,
  event_type VARCHAR(255) NOT NULL
);

DROP TABLE IF EXISTS counter_incremented_event;

CREATE TABLE IF NOT EXISTS counter_incremented_event (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  incremented_by INT NOT NULL
);
