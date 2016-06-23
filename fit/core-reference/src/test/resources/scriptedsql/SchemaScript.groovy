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
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;

// Parameters:
// The connector sends the following:
// action: a string describing the action ("SCHEMA" here)
// log: a handler to the Log facility
// builder: SchemaBuilder instance for the connector
//
// The connector will make the final call to builder.build()
// so the scipt just need to declare the different object types.

// This sample shows how to create 3 basic ObjectTypes: __ACCOUNT__, __GROUP__ and organization.
// Each of them contains one required attribute and normal String attributes


log.info("Entering " + action + " Script");

idAIB = new AttributeInfoBuilder("ID", String.class);
idAIB.setRequired(true);

orgAttrsInfo = new HashSet<AttributeInfo>();
orgAttrsInfo.add(idAIB.build());
orgAttrsInfo.add(AttributeInfoBuilder.build("PRINTERNAME", String.class));
orgAttrsInfo.add(AttributeInfoBuilder.build("LOCATION", String.class));
// Create the organization Object class
ObjectClassInfo ociOrg = new ObjectClassInfoBuilder().setType("__PRINTER__").addAllAttributeInfo(orgAttrsInfo).build();
builder.defineObjectClass(ociOrg);

log.info("Schema script done");