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
SET FOREIGN_KEY_CHECKS = 0;

--SYNCOPE-451 upgrade table definitions to comply with new Apache OpenJpa 2.3.0 version
ALTER TABLE ConnInstance_capabilities CHANGE COLUMN capabilities capabilities VARCHAR(20);
ALTER TABLE ExternalResource CHANGE COLUMN createTraceLevel createTraceLevel VARCHAR(20);
ALTER TABLE ExternalResource CHANGE COLUMN deleteTraceLevel deleteTraceLevel VARCHAR(20);
ALTER TABLE ExternalResource CHANGE COLUMN syncTraceLevel syncTraceLevel VARCHAR(20);
ALTER TABLE ExternalResource CHANGE COLUMN propagationMode propagationMode VARCHAR(20);
ALTER TABLE ExternalResource CHANGE COLUMN updateTraceLevel updateTraceLevel VARCHAR(20);
ALTER TABLE MSchema CHANGE COLUMN type type VARCHAR(20);
ALTER TABLE RMappingItem CHANGE COLUMN intMappingType intMappingType VARCHAR(23);
ALTER TABLE RMappingItem CHANGE COLUMN purpose purpose VARCHAR(20);
ALTER TABLE RSchema CHANGE COLUMN type type VARCHAR(20);
ALTER TABLE SyncopeLogger CHANGE COLUMN logLevel logLevel VARCHAR(20);
ALTER TABLE SyncopeLogger CHANGE COLUMN logType logType VARCHAR(20);
ALTER TABLE SyncopeUser CHANGE COLUMN cipherAlgorithm cipherAlgorithm VARCHAR(20);
ALTER TABLE UMappingItem CHANGE COLUMN intMappingType intMappingType VARCHAR(23);
ALTER TABLE UMappingItem CHANGE COLUMN purpose purpose VARCHAR(20);
ALTER TABLE USchema CHANGE COLUMN type type VARCHAR(20);
ALTER TABLE SyncopeUser CHANGE COLUMN password password VARCHAR(255);

-- SYNCOPE5-524
alter table ExternalResource change xmlConfiguration jsonConf text;
alter table ConnInstance change xmlConfiguration jsonConf text;

-- preliminary steps for upgrade of (refactored) role and membership attributes
alter table RAttr drop foreign key RAttr_ibfk_2;
alter table RDerAttr drop foreign key RDerAttr_ibfk_2;
alter table RVirAttr drop foreign key RVirAttr_ibfk_2;

alter table MAttr drop foreign key MAttr_ibfk_2;
alter table MDerAttr drop foreign key MDerAttr_ibfk_2;
alter table MVirAttr drop foreign key MVirAttr_ibfk_2;

alter table UDerAttr drop foreign key UDerAttr_ibfk_2;
alter table UDerAttr change DERIVEDSCHEMA_NAME DERSCHEMA_NAME VARCHAR(255);
ALTER TABLE UDerAttr ADD CONSTRAINT  UDerAttr_ibfk_2 FOREIGN KEY (DERSCHEMA_NAME) REFERENCES UDerSchema (name);  

alter table UVirAttr drop foreign key UVirAttr_ibfk_2;
alter table UVirAttr change VIRTUALSCHEMA_NAME VIRSCHEMA_NAME VARCHAR(255);
ALTER TABLE UVirAttr ADD CONSTRAINT UVirAttr_ibfk_2 FOREIGN KEY (VIRSCHEMA_NAME) REFERENCES UVirSchema (name);  

-- SYNCOPE-444, SYNCOPE-409, SYNCOPE-445
ALTER TABLE Notification DROP COLUMN xmlAbout;
ALTER TABLE Notification DROP COLUMN xmlRecipients;

-- create a backup of SyncopeConf table into new SyncopeConf_temp table
create table SyncopeConf_temp (confKey varchar(255) primary key, confValue varchar(255));
insert into SyncopeConf_temp (confKey,confValue) select confKey,confValue from SyncopeConf;

-- delete SyncopeConf table, it will be created and initilized respectively by OpenJpa and ContentInitializer
DROP TABLE SyncopeConf;

-- delete views and indexes, they will be recreated by ContentInitializer
DROP VIEW user_search;
DROP VIEW user_search_attr;
DROP VIEW user_search_membership;
DROP VIEW user_search_resource;
DROP VIEW user_search_null_attr;
DROP VIEW user_search_role_resource;
DROP VIEW user_search_unique_attr;
DROP VIEW role_search;
DROP VIEW role_search_attr;
DROP VIEW role_search_entitlements;
DROP VIEW role_search_null_attr;
DROP VIEW role_search_resource;
DROP VIEW role_search_unique_attr;

DROP INDEX UAttrValue_stringvalueIndex ON UAttrValue;
DROP INDEX UAttrValue_datevalueIndex ON UAttrValue; 
DROP INDEX UAttrValue_longvalueIndex ON UAttrValue;
DROP INDEX UAttrValue_doublevalueIndex ON UAttrValue;
DROP INDEX UAttrValue_booleanvalueIndex ON UAttrValue;
DROP INDEX MAttrValue_stringvalueIndex ON MAttrValue;
DROP INDEX MAttrValue_datevalueIndex ON MAttrValue;
DROP INDEX MAttrValue_longvalueIndex ON MAttrValue;
DROP INDEX MAttrValue_doublevalueIndex ON MAttrValue;
DROP INDEX MAttrValue_booleanvalueIndex ON MAttrValue;
DROP INDEX RAttrValue_stringvalueIndex ON RAttrValue;
DROP INDEX RAttrValue_datevalueIndex ON RAttrValue;
DROP INDEX RAttrValue_longvalueIndex ON RAttrValue;
DROP INDEX RAttrValue_doublevalueIndex ON RAttrValue;
DROP INDEX RAttrValue_booleanvalueIndex ON RAttrValue;
DROP INDEX Task_executedIndex ON Task;
DROP INDEX ACT_RU_TASK_PARENT_TASK_ID_ ON ACT_RU_TASK;


SET FOREIGN_KEY_CHECKS = 1;