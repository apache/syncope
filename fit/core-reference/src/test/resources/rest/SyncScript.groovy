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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import java.util.Date;
import javax.ws.rs.core.Response
import org.apache.cxf.jaxrs.client.WebClient
import org.identityconnectors.common.security.GuardedString

// Parameters:
// The connector sends the following:
// client : CXF WebClient
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

def buildConnectorObject(node) {
  return [
    __UID__:node.get("key").textValue(), 
    __NAME__:node.get("key").textValue(),
    __ENABLE__:node.get("status").textValue().equals("ACTIVE"),
    __PASSWORD__:new GuardedString(node.get("password").textValue().toCharArray()),
    key:node.get("key").textValue(),
    username:node.get("username").textValue(),
    firstName:node.get("firstName").textValue(),
    surname:node.get("surname").textValue(),
    email:node.get("email").textValue()
  ];
}

log.info("Entering " + action + " Script");

WebClient webClient = client;
ObjectMapper mapper = new ObjectMapper();

if (action.equalsIgnoreCase("GET_LATEST_SYNC_TOKEN")) {
  switch (objectClass) {
  case "__ACCOUNT__":
    latestToken = new Date().getTime();
    break;
    
  default:
    latestToken = null;
  }

  return latestToken;
} else if (action.equalsIgnoreCase("SYNC")) {
  def result = [];

  switch (objectClass) {
  case "__ACCOUNT__":
    webClient.path("/users/changelog");      
    if (token != null) {
      webClient.query("from", token.toString());            
    }

    log.ok("Sending GET to {0}", webClient.getCurrentURI().toASCIIString());

    Response response = webClient.get();    

    log.ok("CHANGELOG response: {0} {1}", response.getStatus(), response.getHeaders());

    if (response.getStatus() != 200) {
      throw new RuntimeException("Unexpected response from server: " 
        + response.getStatus() + " " + response.getHeaders());
    }
    
    ArrayNode node = mapper.readTree(response.getEntity());
    
    for (i = 0; i < node.size(); i++) {
      if (node.get(i).get("deleted").booleanValue()) {
        result.add([
            operation:"DELETE",
            uid:node.get(i).get("user").get("key").textValue(),
            token:node.get(i).get("lastChangeDate").longValue(),
            attributes:[:]
          ]);        
      } else {
        result.add([
            operation:"CREATE_OR_UPDATE",
            uid:node.get(i).get("user").get("key").textValue(),
            token:node.get(i).get("lastChangeDate").longValue(),
            attributes:buildConnectorObject(node.get(i).get("user"))
          ]);
      }
    }
    break;
  }
  
  log.ok("Sync script: found " + result.size() + " events to sync");
  return result;
} else {
  log.error("Sync script: action '" + action + "' is not implemented in this script");
  return null;
}
