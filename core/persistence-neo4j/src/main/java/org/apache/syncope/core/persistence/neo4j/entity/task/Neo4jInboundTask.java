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
package org.apache.syncope.core.persistence.neo4j.entity.task;

import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.InboundTask;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jInboundTask.NODE)
public abstract class Neo4jInboundTask<T extends InboundTask<T>>
        extends Neo4jProvisioningTask<T> implements InboundTask<T> {

    private static final long serialVersionUID = -4690738171334264804L;

    public static final String NODE = "InboundTask";

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jRealm destinationRealm;

    @NotNull
    private Boolean remediation = false;

    @Override
    public Realm getDestinationRealm() {
        return destinationRealm;
    }

    @Override
    public void setDestinationRealm(final Realm destinationRealm) {
        checkType(destinationRealm, Neo4jRealm.class);
        this.destinationRealm = (Neo4jRealm) destinationRealm;
    }

    @Override
    public void setRemediation(final boolean remediation) {
        this.remediation = remediation;
    }

    @Override
    public boolean isRemediation() {
        return concurrentSettings != null ? true : remediation;
    }
}
