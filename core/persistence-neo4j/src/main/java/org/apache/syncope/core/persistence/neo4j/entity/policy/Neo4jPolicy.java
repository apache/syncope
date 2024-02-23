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
package org.apache.syncope.core.persistence.neo4j.entity.policy;

import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.common.validation.PolicyCheck;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGeneratedKeyNode;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jPolicy.NODE)
@PolicyCheck
public abstract class Neo4jPolicy extends AbstractGeneratedKeyNode implements Policy {

    private static final long serialVersionUID = -5844833125843247458L;

    public static final String NODE = "Policy";

    @NotNull
    private String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }
}
