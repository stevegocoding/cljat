CREATE TABLE messages
(
  msg_id              BIGINT         NOT NULL AUTO_INCREMENT,
  sender_id           BIGINT         NOT NULL,
  dest_id             BIGINT         NOT NULL,
  content             VARCHAR(1024), 
  timestamp           TIMESTAMP      NOT NULL
);
--;;
ALTER TABLE users ADD PRIMARY KEY (msg_id);
--;;
ALTER TABLE messages ADD FOREIGN KEY (sender_id) REFERENCES users(user_id);
--;;
ALTER TABLE messages ADD FOREIGN KEY (dest_id) REFERENCES threads(thread_id);
