CREATE TABLE store (
  id int NOT NULL AUTO_INCREMENT,
  user_id int NOT NULL,
  name varchar(30) NOT NULL,
  content varchar(255) NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE users (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(30) NOT NULL,
  passhash varchar(128) NOT NULL,
  PRIMARY KEY(id)
);