CREATE TABLE IF NOT EXISTS friendships
(
  user_id       INT NOT NULL,
  friend_id     INT NOT NULL
);
--;;
ALTER TABLE friendships ADD PRIMARY KEY (user_id, friend_id);
--;;
ALTER TABLE friendships ADD FOREIGN KEY (user_id) REFERENCES users(user_id);
--;;
ALTER TABLE friendships ADD FOREIGN KEY (friend_id) REFERENCES users(user_id);
