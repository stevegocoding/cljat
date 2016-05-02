CREATE TABLE users
(
  user_id                 BIGINT           NOT NULL AUTO_INCREMENT,
  email                   VARCHAR(254)  NOT NULL,
  nickname                VARCHAR(50)   NOT NULL,
  password                VARCHAR(200)  NOT NULL,
  register_time           TIMESTAMP
);
--;;
ALTER TABLE users ADD PRIMARY KEY (user_id);
