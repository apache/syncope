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
import com.fasterxml.jackson.databind.node.ObjectNode
import javax.ws.rs.core.Response
import org.apache.cxf.jaxrs.client.WebClient
import org.identityconnectors.common.security.GuardedString

// Parameters:
// The connector sends the following:
// client : CXF WebClient
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// action: a string describing the action ("SEARCH" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map
// query: a handler to the Query Map
//
// The Query map describes the filter used (via FIQL's ConditionType):
//
// query = [ operation: "EQUALS", left: attribute, right: "value" ]
// query = [ operation: "GREATER_THAN", left: attribute, right: "value" ]
// query = [ operation: "GREATER_OR_EQUALS", left: attribute, right: "value" ]
// query = [ operation: "LESS_THAN", left: attribute, right: "value" ]
// query = [ operation: "LESS_OR_EQUALS", left: attribute, right: "value" ]
// query = null : then we assume we fetch everything
//
// AND and OR filter just embed a left/right couple of queries.
// query = [ operation: "AND", left: query1, right: query2 ]
// query = [ operation: "OR", left: query1, right: query2 ]
//
// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

def buildConnectorObject(node) {
  return [
    __UID__:node.get("key").textValue(), 
    __NAME__:node.get("key").textValue(),
    username:node.get("username").textValue(),
    password:node.has("password") && node.get("password").textValue() != null
      ? new GuardedString(node.get("password").textValue().toCharArray()) 
      : null,
    firstName:node.get("firstName").textValue(),
    surname:node.get("surname").textValue(),
    email:node.get("email").textValue()
  ];
}

log.info("Entering " + action + " Script");

WebClient webClient = client;
ObjectMapper mapper = new ObjectMapper();

def result = []

switch (objectClass) {
case "__ACCOUNT__":
  if (query == null || (!query.get("left").equals("__UID__") && !query.get("conditionType").equals("EQUALS"))) {
    webClient.path("/users");
    Response response = webClient.get();    
    ArrayNode node = mapper.readTree(response.getEntity());
    
    for (i = 0; i < node.size(); i++) {
      result.add(buildConnectorObject(node.get(i)));
    }
  } else {
    webClient.path("/users/" + query.get("right"));
    Response response = webClient.get();
    if (response.getStatus() == 200) {
      ObjectNode node = mapper.readTree(response.getEntity());
      result.add(buildConnectorObject(node));
    } else {
      log.warn("Could not read object {0}", query.get("right"));
    }
  }

  break

default:
  result;
}

return result;
