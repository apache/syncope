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

IF NOT EXISTS
(SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[logging_event]') AND type in (N'U'))
BEGIN
CREATE TABLE logging_event ( timestmp         DECIMAL(20) NOT NULL, formatted_message  VARCHAR(4000) NOT NULL, logger_name       VARCHAR(254) NOT NULL, level_string      VARCHAR(254) NOT NULL, thread_name       VARCHAR(254), reference_flag    SMALLINT, arg0              VARCHAR(254), arg1              VARCHAR(254), arg2              VARCHAR(254), arg3              VARCHAR(254), caller_filename   VARCHAR(254) NOT NULL, caller_class      VARCHAR(254) NOT NULL, caller_method     VARCHAR(254) NOT NULL, caller_line       CHAR(4) NOT NULL, event_id          DECIMAL(38) NOT NULL identity, PRIMARY KEY(event_id))
END;;

IF NOT EXISTS
(SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[logging_event_property]') AND type in (N'U'))
BEGIN
CREATE TABLE logging_event_property ( event_id          DECIMAL(38) NOT NULL, mapped_key        VARCHAR(254) NOT NULL, mapped_value      VARCHAR(1024), PRIMARY KEY(event_id, mapped_key), FOREIGN KEY (event_id) REFERENCES logging_event(event_id))
END;;

IF NOT EXISTS
(SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[logging_event_exception]') AND type in (N'U'))
BEGIN
CREATE TABLE logging_event_exception ( event_id         DECIMAL(38) NOT NULL, i                SMALLINT NOT NULL, trace_line       VARCHAR(254) NOT NULL, PRIMARY KEY(event_id, i), FOREIGN KEY (event_id) REFERENCES logging_event(event_id) ) 
END;;
