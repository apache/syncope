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

CREATE TABLE AuditEntry (
  EVENT_DATE TIMESTAMP,
  LOGGER_LEVEL VARCHAR(255) NOT NULL,
  LOGGER VARCHAR(255) NOT NULL,
  MESSAGE CLOB CHECK (MESSAGE IS JSON) NOT NULL,
  THROWABLE CLOB
);

-- The following index require Oracle TEXT to be installed on the given Oracle database:
-- http://dbaflavours.blogspot.com/2012/09/ora-29833-indextype-does-not-exist_18.html
CREATE SEARCH INDEX AuditEntry_MESSAGE_Index ON AuditEntry(MESSAGE) FOR JSON;
