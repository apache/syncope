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
import groovy.sql.DataSet;

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

log.info("Entering " + action + " Script");
def sql = new Sql(connection);

if (action.equalsIgnoreCase("GET_LATEST_SYNC_TOKEN")) {
  switch (objectClass) {
  case "__PRINTER__":
    row = sql.firstRow("SELECT lastmodification FROM TESTPRINTER ORDER BY lastmodification DESC");
    log.ok("Get Latest Sync Token script: last token is: " + row["lastmodification"])
    break;
    
  default:
    row = null;
  }

  return row == null ? null : row["lastmodification"].getTime();
} else if (action.equalsIgnoreCase("SYNC")) {
  def result = [];
  def lastmodification = null;
  if (token != null) {
    lastmodification = new Date(token);
  } else {
    lastmodification = new Date(0);
  }

  switch (objectClass) {
  case "__PRINTER__":
    sql.eachRow("SELECT * FROM TESTPRINTER WHERE lastmodification > ${lastmodification}",
      {
        result.add([
            operation:it.deleted ? "DELETE": "CREATE_OR_UPDATE", 
            uid:it.id.toString(), 
            token:it.lastmodification.getTime(), 
            attributes:[
              __UID__:it.id.toString(),
              __NAME__:it.id.toString(),
              ID:it.id.toString(),
              PRINTERNAME:it.printername,
              LOCATION:it.location
            ]
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
