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
package org.apache.syncope.ext.scimv2.api.type;

public enum Resource {

    ServiceProviderConfig("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"),
    ResourceType("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
    Schema("urn:ietf:params:scim:schemas:core:2.0:Schema"),
    User("urn:ietf:params:scim:schemas:core:2.0:User"),
    EnterpriseUser("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"),
    ExtensionUser("urn:ietf:params:scim:schemas:extension:syncope:2.0:User"),
    Group("urn:ietf:params:scim:schemas:core:2.0:Group"),
    SearchRequest("urn:ietf:params:scim:api:messages:2.0:SearchRequest"),
    ListResponse("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
    PatchOp("urn:ietf:params:scim:api:messages:2.0:PatchOp"),
    Error("urn:ietf:params:scim:api:messages:2.0:Error");

    private final String schema;

    Resource(final String schema) {
        this.schema = schema;
    }

    public String schema() {
        return schema;
    }

}
