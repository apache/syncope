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
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.common.security.GuardedString;

// Parameters:
// The connector sends the following:
// connection: handler to the SQL connection
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// action: a string describing the action ("SEARCH" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map
// query: a handler to the Query Map
//
// The Query map describes the filter used.
//
// query = [ operation: "CONTAINS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "ENDSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "STARTSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "EQUALS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = null : then we assume we fetch everything
//
// AND and OR filter just embed a left/right couple of queries.
// query = [ operation: "AND", left: query1, right: query2 ]
// query = [ operation: "OR", left: query1, right: query2 ]
//
// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

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
    item.put(it.schema_name, value);
    withValues.add(it.schema_name);
  }
}

log.ok("Entering " + action + " script");

def sql = new Sql(connection);
def result = []

def queryAppend = "";
def pageSize = options[OperationOptions.OP_PAGE_SIZE];
if (pageSize) {
  queryAppend += " LIMIT " + pageSize;
}
def pagedResultsCookie = options[OperationOptions.OP_PAGED_RESULTS_COOKIE];
if (pagedResultsCookie) {
  queryAppend += " OFFSET " + pagedResultsCookie;
}

switch ( objectClass ) {
case "__ACCOUNT__":
  sql.eachRow("SELECT * FROM USER_SEARCH" + queryAppend, {
      item = [
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
          item.put(attr, null);          
        }
      }
      
      resources = [];
      sql.eachRow("SELECT DISTINCT * FROM USER_SEARCH_RESOURCE WHERE subject_id = " + it.id, {
          resources.add(it.resource_name);
        });      
      item.put('__RESOURCES__', resources);
      
      result.add(item)
    });

  if (result.size() == pageSize) {
    if (pagedResultsCookie == "") {
      pagedResultsCookie = pagedResultsCookie + result.size();
    } else {
      pagedResultsCookie = Integer.toString(pagedResultsCookie.toInteger() + result.size());
    }
  }
  break

case "__GROUP__":
  sql.eachRow("SELECT * FROM ROLE_SEARCH" + queryAppend, {
      name = it.id + ' ' + it.name;
      item = [
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
          item.put(attr, null);          
        }
      }

      resources = [];
      sql.eachRow("SELECT DISTINCT * FROM ROLE_SEARCH_RESOURCE WHERE subject_id = " + it.id, {
          resources.add(it.resource_name);
        });      
      item.put('__RESOURCES__', resources);

      memberships = [];
      sql.eachRow("SELECT u.username as username FROM USER_SEARCH_MEMBERSHIP usm, USER_SEARCH u "
        +"WHERE u.subject_id=usm.subject_id AND usm.role_id =" + it.id, {
          memberships.add(it.username);
        });      
      item.put('__MEMBERSHIPS__', memberships);

      result.add(item)
    });
  break
  
default:
  result;
}

log.ok(action + " script done");

return result;
