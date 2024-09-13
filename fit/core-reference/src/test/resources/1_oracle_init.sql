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
ALTER SESSION SET CONTAINER=FREEPDB1;

CREATE TABLESPACE syncopetwo DATAFILE 'syncopetwo.dat' SIZE 10M AUTOEXTEND ON;
CREATE USER syncopetwo IDENTIFIED BY syncopetwo DEFAULT TABLESPACE syncopetwo QUOTA UNLIMITED ON USERS;

CREATE TABLESPACE syncopetest DATAFILE 'syncopetest.dat' SIZE 10M AUTOEXTEND ON;
CREATE USER syncopetest IDENTIFIED BY syncopetest DEFAULT TABLESPACE syncopetest QUOTA UNLIMITED ON USERS;
