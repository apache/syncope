
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import groovy.sql.Sql;
import org.identityconnectors.common.security.GuardedString

// Parameters:
// The connector sends the following:
// connection: handler to the SQL connection
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// action: a string describing the action ("SYNC" or "GET_LATEST_SYNC_TOKEN" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map (null if action = "GET_LATEST_SYNC_TOKEN")
// token: a handler to an Object representing the sync token (null if action = "GET_LATEST_SYNC_TOKEN")
//
//
// Returns:
// if action = "GET_LATEST_SYNC_TOKEN", it must return an object representing the last known
// sync token for the corresponding ObjectClass
// 
// if action = "SYNC":
// A list of Maps . Each map describing one update:
// Map should look like the following:
//
// [
// "token": <Object> token object (could be Integer, Date, String) , [!! could be null]
// "operation":<String> ("CREATE_OR_UPDATE"|"DELETE")  will always default to CREATE_OR_DELETE ,
// "uid":<String> uid  (uid of the entry) ,
// "previousUid":<String> prevuid (This is for rename ops) ,
// "password":<String> password (optional... allows to pass clear text password if needed),
// "attributes":Map<String,List> of attributes name/values
// ]

def addAttributes(it, withValues) {
  value = it.stringValue;
  if (value == null) {
    value = it.longValue;
  }
  if (value == null) {
    value = it.doubleValue;
  }
  if (value == null) {
    if (it.dateValue instanceof java.sql.Timestamp) {
      value = new Date(it.dateValue.getTime()).format("yyyy-MM-dd HH:mm:ss");
    }
  }
  if (value == null) {
    value = it.booleanValue;
  }
          
  if (options[OperationOptions.OP_ATTRIBUTES_TO_GET].contains(it.schema_name)) {
    attributes.put(it.schema_name, value);
    withValues.add(it.schema_name);
  }
}

log.ok("Entering " + action + " script");

def sql = new Sql(connection);

if (action.equalsIgnoreCase("GET_LATEST_SYNC_TOKEN")) {
  switch (objectClass) {
  case "__ACCOUNT__":    
    row = sql.firstRow("SELECT lastChangeDate FROM USER_SEARCH ORDER BY lastChangeDate DESC");
    log.ok("Get Latest Sync Token script: last token is: " + row["lastChangeDate"])
    break;
    
  case "__GROUP__":    
    row = sql.firstRow("SELECT lastChangeDate FROM ROLE_SEARCH ORDER BY lastChangeDate DESC");
    log.ok("Get Latest Sync Token script: last token is: " + row["lastChangeDate"])
    break;

  default:
    row = null;
  }

  return row == null ? null : row["lastChangeDate"].getTime();
} else if (action.equalsIgnoreCase("SYNC")) {
  def result = [];
  def lastChangeDate = null;
  if (token == null) {
    lastChangeDate = new Date(0);
  } else {
    lastChangeDate = new Date(token);
  }

  switch (objectClass) {
  case "__ACCOUNT__":    
    sql.eachRow("SELECT * FROM USER_SEARCH WHERE lastChangeDate > ${lastChangeDate}",
      {
        attributes = [
          __UID__: it.username, 
          __NAME__: it.username, 
          username: it.username,
          __PASSWORD__: new GuardedString(it.password.toCharArray()),
          cipherAlgorithm: it.cipherAlgorithm,
          __ENABLE__: it.suspended == 0
        ];

        withValues = ['__UID__', '__NAME__', '__PASSWORD__', 'cipherAlgorithm', '__ENABLE__', 'username'];
      
        sql.eachRow("SELECT * FROM USER_SEARCH_ATTR WHERE subject_id = " + it.id, {
            addAttributes(it, withValues);
          });
        sql.eachRow("SELECT * FROM USER_SEARCH_UNIQUE_ATTR WHERE subject_id = " + it.id, {
            addAttributes(it, withValues);
          });
      
        for (attr in options[OperationOptions.OP_ATTRIBUTES_TO_GET]) {
          if (!withValues.contains(attr)) {
            attributes.put(attr, null);          
          }
        }
      
        resources = [];
        sql.eachRow("SELECT DISTINCT * FROM USER_SEARCH_RESOURCE WHERE subject_id = " + it.id, {
            resources.add(it.resource_name);
          });      
        attributes.put('__RESOURCES__', resources);

        result.add([
            operation:"CREATE_OR_UPDATE", 
            uid: it.username, 
            token: it.lastChangeDate.getTime(), 
            attributes: attributes
          ]);
      }
    )
    break;
  
  case "__GROUP__":    
    sql.eachRow("SELECT * FROM ROLE_SEARCH WHERE lastChangeDate > ${lastChangeDate}",
      {
        name = it.id + ' ' + it.name;
        attributes = [
          __UID__: name, 
          __NAME__: name, 
          __ENABLE__: true, 
          name: name
        ];
      
        withValues = ['__UID__', '__NAME__', '__ENABLE__', 'name'];
      
        sql.eachRow("SELECT * FROM ROLE_SEARCH_ATTR WHERE subject_id = " + it.id, {
            addAttributes(it, withValues);
          });
        sql.eachRow("SELECT * FROM ROLE_SEARCH_UNIQUE_ATTR WHERE subject_id = " + it.id, {
            addAttributes(it, withValues);
          });
      
        for (attr in options[OperationOptions.OP_ATTRIBUTES_TO_GET]) {
          if (!withValues.contains(attr)) {
            attributes.put(attr, null);          
          }
        }

        resources = [];
        sql.eachRow("SELECT DISTINCT * FROM ROLE_SEARCH_RESOURCE WHERE subject_id = " + it.id, {
            resources.add(it.resource_name);
          });      
        attributes.put('__RESOURCES__', resources);

        memberships = [];
        sql.eachRow("SELECT u.username as username FROM USER_SEARCH_MEMBERSHIP usm, USER_SEARCH u "
          +"WHERE u.subject_id=usm.subject_id AND usm.role_id =" + it.id, {
            memberships.add(it.username);
          });      
        item.put('__MEMBERSHIPS__', memberships);

        result.add([
            operation:"CREATE_OR_UPDATE", 
            uid: name, 
            token: it.lastChangeDate.getTime(), 
            attributes: attributes
          ]);
      }
    )
    break;
  }
  
  log.ok("Sync script: found " + result.size() + " events to sync");
  return result;
} else {
  log.error("Sync script: action '" + action + "' is not implemented in this script");
  return null;
}
