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
// The connector sends us the following:
// connection : SQL connection
//
// action: String correponding to the action (UPDATE/ADD_ATTRIBUTE_VALUES/REMOVE_ATTRIBUTE_VALUES)
//   - UPDATE : For each input attribute, replace all of the current values of that attribute
//     in the target object with the values of that attribute.
//   - ADD_ATTRIBUTE_VALUES: For each attribute that the input set contains, add to the current values
//     of that attribute in the target object all of the values of that attribute in the input set.
//   - REMOVE_ATTRIBUTE_VALUES: For each attribute that the input set contains, remove from the current values
//     of that attribute in the target object any value that matches one of the values of the attribute from the input set.
//   - UPDATE_DELTA: Three input maps are provided: valuesToAdd, valuesToRemove and valuesToReplace
//     For each map key, perform the corresponding actions on attribute values

// log: a handler to the Log facility
//
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
//
// uid: a String representing the entry uid
//
// attributes: an Attribute Map, containg the <String> attribute name as a key and the <List> attribute value(s) as value
// or
// valuesToAdd, valuesToRemove and valuesToReplace (for UPDATE_DELTA): similar data structure
//
// password: password string, clear text (only for UPDATE)
//
// options: a handler to the OperationOptions Map

log.info("Entering " + action + " Script");
def sql = new Sql(connection);


switch (action) {
case "UPDATE":
  if (attributes.get("LOCATION") != null && !attributes.get("LOCATION").isEmpty() && attributes.get("LOCATION").get(0) != null) {
    sql.executeUpdate("UPDATE TESTPRINTER SET printername = ?, location = ?, lastmodification = ? where id = ?", 
      [
        attributes.get("PRINTERNAME").get(0), 
        attributes.get("LOCATION").get(0), 
        new Date(), 
        attributes.get("__NAME__").get(0)
      ])

    return attributes.get("__NAME__").get(0);
  }

  return uid
  break

case "UPDATE_DELTA":
  if (valuesToAdd != null && valuesToAdd.containsKey("paperformat")) {
    for (paperformat: valuesToAdd.get("paperformat")) {
      sql.executeUpdate("INSERT INTO TESTPRINTER_PAPERFORMAT(printer_id, paper_format) VALUES(?, ?)",
        [uid, paperformat])
    }
  }
  if (valuesToRemove != null && valuesToRemove.containsKey("paperformat")) {
    for (paperformat: valuesToRemove.get("paperformat")) {
      sql.executeUpdate("DELETE FROM TESTPRINTER_PAPERFORMAT WHERE printer_id = ? AND paper_format = ?",
        [uid, paperformat])
    }    
  }
  
  return uid
  break

default:
  sql
}
