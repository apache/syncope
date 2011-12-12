ALTER USER sa SET PASSWORD '${testdb.password}';

DROP TABLE test IF EXISTS;
CREATE TABLE test (
id INTEGER PRIMARY KEY,
password VARCHAR(255) NOT NULL,
status VARCHAR(5));

INSERT INTO test VALUES (1, 'password', 'false');
