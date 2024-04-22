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
import java.util.Optional;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ThreadPoolSettings;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.common.validation.ProvisioningTaskCheck;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jProvisioningTask.NODE)
@ProvisioningTaskCheck
public abstract class Neo4jProvisioningTask<T extends SchedTask>
        extends Neo4jSchedTask implements ProvisioningTask<T> {

    private static final long serialVersionUID = -4141057723006682562L;

    public static final String NODE = "ProvisioningTask";

    @NotNull
    private Boolean performCreate = false;

    @NotNull
    private Boolean performUpdate = false;

    @NotNull
    private Boolean performDelete = false;

    @NotNull
    private Boolean syncStatus = false;

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jExternalResource resource;

    /**
     * @see UnmatchingRule
     */
    @NotNull
    protected UnmatchingRule unmatchingRule;

    /**
     * @see MatchingRule
     */
    @NotNull
    protected MatchingRule matchingRule;

    protected String concurrentSettings;

    @Override
    public boolean isPerformCreate() {
        return performCreate;
    }

    @Override

    public void setPerformCreate(final boolean performCreate) {
        this.performCreate = performCreate;
    }

    @Override

    public boolean isPerformUpdate() {
        return performUpdate;
    }

    @Override

    public void setPerformUpdate(final boolean performUpdate) {
        this.performUpdate = performUpdate;
    }

    @Override
    public boolean isPerformDelete() {
        return performDelete;
    }

    @Override
    public void setPerformDelete(final boolean performDelete) {
        this.performDelete = performDelete;
    }

    @Override
    public boolean isSyncStatus() {
        return syncStatus;
    }

    @Override
    public void setSyncStatus(final boolean syncStatus) {
        this.syncStatus = syncStatus;
    }

    @Override
    public UnmatchingRule getUnmatchingRule() {
        return this.unmatchingRule;
    }

    @Override
    public void setUnmatchingRule(final UnmatchingRule unmatchigRule) {
        this.unmatchingRule = unmatchigRule;
    }

    @Override
    public MatchingRule getMatchingRule() {
        return this.matchingRule;
    }

    @Override
    public void setMatchingRule(final MatchingRule matchigRule) {
        this.matchingRule = matchigRule;
    }

    @Override
    public ThreadPoolSettings getConcurrentSettings() {
        return Optional.ofNullable(concurrentSettings).
                map(s -> POJOHelper.deserialize(s, ThreadPoolSettings.class)).orElse(null);
    }

    @Override
    public void setConcurrentSettings(final ThreadPoolSettings settings) {
        this.concurrentSettings = Optional.ofNullable(settings).map(POJOHelper::serialize).orElse(null);
    }

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        checkType(resource, Neo4jExternalResource.class);
        this.resource = (Neo4jExternalResource) resource;
    }
}
