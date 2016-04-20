-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at

--  http://www.apache.org/licenses/LICENSE-2.0

-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
ALTER USER sa SET PASSWORD '${testdb.password}';

DROP TABLE test IF EXISTS;
CREATE TABLE test (
id VARCHAR(50) PRIMARY KEY,
password VARCHAR(255) NOT NULL,
status VARCHAR(5));

INSERT INTO test VALUES ('testuser1', 'password', 'false');

-- this table must be created in order to provide a specific test for issueSYNCOPE68
DROP TABLE test2 IF EXISTS;
CREATE TABLE test2 (
id VARCHAR(50) PRIMARY KEY,
password VARCHAR(255) NOT NULL,
status VARCHAR(5));

INSERT INTO test2 VALUES ('testuser2', 'password321', 'false');
INSERT INTO test2 VALUES ('rossini', 'password321', 'true');
INSERT INTO test2 VALUES ('verdi', 'password321', 'true');

-- this table is for issueSYNCOPE230
DROP TABLE testpull IF EXISTS;
CREATE TABLE testpull (
id CHAR(36) PRIMARY KEY,
username VARCHAR(80),
surname VARCHAR(80),
email VARCHAR(80));

INSERT INTO testpull VALUES ('a54b3794-b231-47be-b24a-11e1a42949f6', 'issuesyncope230', 'Surname', 'syncope230@syncope.apache.org');

DROP TABLE testPRINTER IF EXISTS;
CREATE TABLE testPRINTER (
id CHAR(36) PRIMARY KEY,
location VARCHAR(80),
lastModification TIMESTAMP);
