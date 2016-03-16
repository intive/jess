create user jess password 'jess';
create database jess owner jess;

CREATE TABLE IF NOT EXISTS metadata (
  persistence_key BIGSERIAL NOT NULL,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_nr BIGINT NOT NULL,
  PRIMARY KEY (persistence_key),
  UNIQUE (persistence_id)
);
alter table metadata owner to jess;

CREATE TABLE IF NOT EXISTS journal (
  persistence_key BIGINT NOT NULL REFERENCES metadata(persistence_key),
  sequence_nr BIGINT NOT NULL,
  message BYTEA NOT NULL,
  PRIMARY KEY (persistence_key, sequence_nr)
);
 alter table journal owner to jess;

CREATE TABLE IF NOT EXISTS snapshot (
  persistence_key BIGINT NOT NULL REFERENCES metadata(persistence_key),
  sequence_nr BIGINT NOT NULL,
  created_at BIGINT NOT NULL,
  snapshot BYTEA NOT NULL,
  PRIMARY KEY (persistence_key, sequence_nr)
);
alter table snapshot owner to jess;
