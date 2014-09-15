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

DROP INDEX UAttrValue_stringvalueIndex;
DROP INDEX UAttrValue_datevalueIndex; 
DROP INDEX UAttrValue_longvalueIndex;
DROP INDEX UAttrValue_doublevalueIndex;
DROP INDEX UAttrValue_booleanvalueIndex;
DROP INDEX MAttrValue_stringvalueIndex;
DROP INDEX MAttrValue_datevalueIndex;
DROP INDEX MAttrValue_longvalueIndex;
DROP INDEX MAttrValue_doublevalueIndex;
DROP INDEX MAttrValue_booleanvalueIndex;
DROP INDEX RAttrValue_stringvalueIndex;
DROP INDEX RAttrValue_datevalueIndex;
DROP INDEX RAttrValue_longvalueIndex;
DROP INDEX RAttrValue_doublevalueIndex;
DROP INDEX RAttrValue_booleanvalueIndex;
DROP INDEX Task_executedIndex;
DROP INDEX ACT_RU_TASK_PARENT_TASK_ID_;

--SYNCOPE-451 upgrade table definitions to comply with new Apache OpenJpa 2.3.0 version
ALTER TABLE ConnInstance_capabilities ALTER COLUMN capabilities TYPE VARCHAR(20);
ALTER TABLE ExternalResource ALTER COLUMN createTraceLevel TYPE VARCHAR(20);
ALTER TABLE ExternalResource ALTER COLUMN deleteTraceLevel TYPE VARCHAR(20);
ALTER TABLE ExternalResource ALTER COLUMN syncTraceLevel TYPE VARCHAR(20);
ALTER TABLE ExternalResource ALTER COLUMN propagationMode TYPE VARCHAR(20);
ALTER TABLE ExternalResource ALTER COLUMN updateTraceLevel TYPE VARCHAR(20);
ALTER TABLE MSchema ALTER COLUMN type TYPE VARCHAR(20);
ALTER TABLE RMappingItem ALTER COLUMN intMappingType TYPE VARCHAR(23);
ALTER TABLE RMappingItem ALTER COLUMN purpose TYPE VARCHAR(20);
ALTER TABLE RSchema ALTER COLUMN type TYPE VARCHAR(20);
ALTER TABLE SyncopeLogger ALTER COLUMN logLevel TYPE VARCHAR(20);
ALTER TABLE SyncopeLogger ALTER COLUMN logType TYPE VARCHAR(20);
ALTER TABLE SyncopeUser ALTER COLUMN cipherAlgorithm TYPE VARCHAR(20);
ALTER TABLE UMappingItem ALTER COLUMN intMappingType TYPE VARCHAR(23);
ALTER TABLE UMappingItem ALTER COLUMN purpose TYPE VARCHAR(20);
ALTER TABLE USchema ALTER COLUMN type TYPE VARCHAR(20);
ALTER TABLE SyncopeUser ALTER COLUMN password TYPE VARCHAR(255);

-- SYNCOPE5-524
ALTER TABLE ExternalResource RENAME COLUMN xmlConfiguration TO jsonConf;
ALTER TABLE ConnInstance RENAME COLUMN xmlConfiguration TO jsonConf;

-- preliminary steps for upgrade of (refactored) role and membership attributes
ALTER TABLE RAttr DROP CONSTRAINT rattr_schema_name_fkey;
ALTER TABLE RDerAttr DROP CONSTRAINT rderattr_derivedschema_name_fkey;
ALTER TABLE RVirAttr DROP CONSTRAINT rvirattr_virtualschema_name_fkey;

ALTER TABLE MAttr DROP CONSTRAINT mattr_schema_name_fkey;
ALTER TABLE MDerAttr DROP CONSTRAINT mderattr_derivedschema_name_fkey;
ALTER TABLE MVirAttr DROP CONSTRAINT mvirattr_virtualschema_name_fkey;

ALTER TABLE UDerAttr DROP CONSTRAINT uderattr_derivedschema_name_fkey;
ALTER TABLE UDerAttr RENAME DERIVEDSCHEMA_NAME TO DERSCHEMA_NAME;
ALTER TABLE UDerAttr ADD CONSTRAINT uderattr_derschema_name_fkey FOREIGN KEY (DERSCHEMA_NAME) REFERENCES UDerSchema (name);  

ALTER TABLE UVirAttr DROP CONSTRAINT uvirattr_virtualschema_name_fkey;
ALTER TABLE UVirAttr RENAME VIRTUALSCHEMA_NAME TO VIRSCHEMA_NAME;
ALTER TABLE UVirAttr ADD CONSTRAINT uvirattr_virschema_name_fkey FOREIGN KEY (VIRSCHEMA_NAME) REFERENCES UVirSchema (name);  

-- SYNCOPE-444, SYNCOPE-409, SYNCOPE-445
ALTER TABLE Notification DROP COLUMN xmlAbout;
ALTER TABLE Notification DROP COLUMN xmlRecipients;

-- create a backup of SyncopeConf table into new SyncopeConf_temp table
create table SyncopeConf_temp (confKey varchar(255) primary key, confValue varchar(255));
insert into SyncopeConf_temp (confKey,confValue) select confKey,confValue from SyncopeConf;

-- delete SyncopeConf table, it will be created and initilized respectively by OpenJpa and ContentInitializer
DROP TABLE SyncopeConf;
