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
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;

// Parameters:
// The connector sends the following:
// connection: handler to the SQL connection
// action: a string describing the action ("SCHEMA" here)
// log: a handler to the Log facility
// builder: SchemaBuilder instance for the connector
//
// The connector will make the final call to builder.build()
// so the scipt just need to declare the different object types.

def getAIB(it) {
  aib = new AttributeInfoBuilder(it.NAME);
    
  switch(it.TYPE) {
  case "String":
  case "Date":
  case "Enum":
    aib.setType(String.class);
    break;
      
  case "Long":
    aib.setType(Long.class);
    break;
        
  case "Double":
    aib.setType(Double.class);
    break;

  case "Boolean":
    aib.setType(Boolean.class);
    break;
  
  case "Encrypted":
    aib.setType(GuardedString.class);
    
  case "Binary":
    aib.setType(byte[].class);
  }
    
  if (it.MULTIVALUE == 1) {
    aib.setMultiValued(true);
  }
    
  if (it.MANDATORYCONDITION == "true") {
    aib.setRequired(true);
  }

  return aib;
}

log.ok("Entering " + action + " script");

// User
idAIB = new AttributeInfoBuilder("USERNAME", String.class);
idAIB.setRequired(true);

userAI = new HashSet<AttributeInfo>();
userAI.add(idAIB.build());

def sql = new Sql(connection);
sql.eachRow("SELECT NAME, TYPE, MULTIVALUE, MANDATORYCONDITION FROM USCHEMA", {    
    userAI.add(getAIB(it).build());
  });

ObjectClassInfo userCI = new ObjectClassInfoBuilder().setType("__ACCOUNT__").addAllAttributeInfo(userAI).build();
builder.defineObjectClass(userCI);

// Group
idAIB = new AttributeInfoBuilder("NAME", String.class);
idAIB.setRequired(true);

roleAI = new HashSet<AttributeInfo>();
roleAI.add(idAIB.build());

sql.eachRow("SELECT NAME, TYPE, MULTIVALUE, MANDATORYCONDITION FROM RSCHEMA", {    
    roleAI.add(getAIB(it).build());
  });

ObjectClassInfo roleCI = new ObjectClassInfoBuilder().setType("__GROUP__").addAllAttributeInfo(roleAI).build();
builder.defineObjectClass(roleCI);

log.ok(action + " script done");
