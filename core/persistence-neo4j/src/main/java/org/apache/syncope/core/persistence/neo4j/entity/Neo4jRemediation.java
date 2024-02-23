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
package org.apache.syncope.core.persistence.neo4j.entity;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.common.validation.RemediationCheck;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPullTask;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jRemediation.NODE)
@RemediationCheck
public class Neo4jRemediation extends AbstractGeneratedKeyNode implements Remediation {

    private static final long serialVersionUID = -1612530286294448682L;

    public static final String NODE = "Remediation";

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyType anyType;

    @NotNull
    private ResourceOperation operation;

    @NotNull
    private String payload;

    @NotNull
    private String error;

    @NotNull
    private OffsetDateTime instant;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jPullTask pullTask;

    @NotNull
    private String remoteName;

    @Override
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, Neo4jAnyType.class);
        this.anyType = (Neo4jAnyType) anyType;
    }

    @Override
    public ResourceOperation getOperation() {
        return operation;
    }

    @Override
    public void setOperation(final ResourceOperation operation) {
        this.operation = operation;
    }

    @Override
    public <C extends AnyCR> C getPayloadAsCR(final Class<C> reference) {
        return POJOHelper.deserialize(this.payload, reference);
    }

    @Override
    public <U extends AnyUR> U getPayloadAsUR(final Class<U> reference) {
        return POJOHelper.deserialize(this.payload, reference);
    }

    @Override
    public String getPayloadAsKey() {
        return this.payload;
    }

    @Override
    public void setPayload(final AnyCR anyCR) {
        this.payload = POJOHelper.serialize(anyCR);
    }

    @Override
    public void setPayload(final AnyUR anyUR) {
        this.payload = POJOHelper.serialize(anyUR);
    }

    @Override
    public void setPayload(final String key) {
        this.payload = key;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public void setError(final String error) {
        this.error = error;
    }

    @Override
    public OffsetDateTime getInstant() {
        return instant;
    }

    @Override
    public void setInstant(final OffsetDateTime instant) {
        this.instant = instant;
    }

    @Override
    public PullTask getPullTask() {
        return pullTask;
    }

    @Override
    public void setPullTask(final PullTask pullTask) {
        checkType(pullTask, Neo4jPullTask.class);
        this.pullTask = (Neo4jPullTask) pullTask;
    }

    @Override
    public String getRemoteName() {
        return remoteName;
    }

    @Override
    public void setRemoteName(final String remoteName) {
        this.remoteName = remoteName;
    }
}
