-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

CREATE TABLESPACE SYNCOPE
  DATAFILE 'SYNCOPE.dat'
  SIZE 10M AUTOEXTEND on;

CREATE TEMPORARY TABLESPACE SYNCOPE_TEMP
  TEMPFILE 'SYNCOPE_TEMP.dat'
  SIZE 5M AUTOEXTEND on;

CREATE USER syncope
  IDENTIFIED BY syncope
  DEFAULT TABLESPACE SYNCOPE
  TEMPORARY TABLESPACE SYNCOPE_TEMP
  QUOTA 256M on SYNCOPE;

GRANT create session TO syncope;
GRANT create table TO syncope;
GRANT create view TO syncope;
GRANT create any trigger TO syncope;
GRANT create any procedure TO syncope;
GRANT create sequence TO syncope;
GRANT create synonym TO syncope;

CREATE DATABASE syncope
  CHARACTER SET US7ASCII
  DEFAULT TABLESPACE SYNCOPE
  DEFAULT TEMPORARY TABLESPACE SYNCOPE_TEMP
