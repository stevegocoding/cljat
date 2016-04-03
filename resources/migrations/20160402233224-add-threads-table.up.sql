CREATE TABLE IF NOT EXISTS threads
(
  thread_id               INT           NOT NULL AUTO_INCREMENT,
  title                   VARCHAR(254)  NOT NULL,
  created_time            TIMESTAMP     NOT NULL
);
--;;
ALTER TABLE threads ADD PRIMARY KEY (thread_id);
