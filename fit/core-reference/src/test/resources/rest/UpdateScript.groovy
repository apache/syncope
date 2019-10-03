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
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.cxf.jaxrs.client.WebClient
import javax.ws.rs.core.Response

// Parameters:
// The connector sends us the following:
// client : CXF WebClient
//
// action: String correponding to the action (UPDATE/ADD_ATTRIBUTE_VALUES/REMOVE_ATTRIBUTE_VALUES)
//   - UPDATE : For each input attribute, replace all of the current values of that attribute
//     in the target object with the values of that attribute.
//   - ADD_ATTRIBUTE_VALUES: For each attribute that the input set contains, add to the current values
//     of that attribute in the target object all of the values of that attribute in the input set.
//   - REMOVE_ATTRIBUTE_VALUES: For each attribute that the input set contains, remove from the current values
//     of that attribute in the target object any value that matches one of the values of the attribute from the input set.

// log: a handler to the Log facility
//
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
//
// uid: a String representing the entry uid
//
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
//
// password: password string, clear text (only for UPDATE)
//
// options: a handler to the OperationOptions Map

log.info("Entering " + action + " Script");

WebClient webClient = client;
ObjectMapper mapper = new ObjectMapper();

assert uid != null

switch (action) {
case "UPDATE":
  switch (objectClass) {  
  case "__ACCOUNT__":
    ObjectNode node = mapper.createObjectNode();
    node.set("key", node.textNode(uid));
    if (attributes.containsKey("__NAME__")) {
      node.set("username", node.textNode(attributes.get("__NAME__").get(0)));
    }
    if (attributes.containsKey("username")) {
      node.set("username", node.textNode(attributes.get("username").get(0)));
    }
    if (password != null) {
      node.set("password", node.textNode(password));
    }
    if (attributes.containsKey("firstName")) {
      node.set("firstName", node.textNode(attributes.get("firstName").get(0)));
    }
    if (attributes.containsKey("surname")) {
      node.set("surname", node.textNode(attributes.get("surname").get(0)));
    }
    if (attributes.containsKey("email")) {
      node.set("email", node.textNode(attributes.get("email").get(0)));
    }
    
    String payload = mapper.writeValueAsString(node);

    // this if update works with PUT
    webClient.path("users").path(uid);
    
    log.ok("Sending PUT to {0} with payload {1}", webClient.getCurrentURI().toASCIIString(), payload);
    
    Response response = webClient.put(payload);
    
    log.ok("Update response: {0} {1}", response.getStatus(), response.getHeaders());
  
    // this instead if update works with PATCH
    //webClient.path("users").path(uid);
    //WebClient.getConfig(webClient).getRequestContext().put("use.async.http.conduit", true);
    //webClient.invoke("PATCH", payload);

  default:
    break
  }

  return uid;
  break

case "ADD_ATTRIBUTE_VALUES":
  break


default:
  break
}