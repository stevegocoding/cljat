CREATE TABLE IF NOT EXISTS users_threads
(
  user_id                 INT           NOT NULL,
  thread_id               INT           NOT NULL
);
--;;
ALTER TABLE users_threads ADD PRIMARY KEY (user_id, thread_id);
--;;
ALTER TABLE users_threads ADD FOREIGN KEY (user_id) REFERENCES users(user_id);
--;;
ALTER TABLE users_threads ADD FOREIGN KEY (thread_id) REFERENCES threads(thread_id);
