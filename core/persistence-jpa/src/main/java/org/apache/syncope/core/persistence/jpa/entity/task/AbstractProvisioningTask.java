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
package org.apache.syncope.core.persistence.jpa.entity.task;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.jpa.validation.entity.ProvisioningTaskCheck;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;

@MappedSuperclass
@ProvisioningTaskCheck
public abstract class AbstractProvisioningTask extends AbstractSchedTask implements ProvisioningTask {

    private static final long serialVersionUID = -4141057723006682562L;

    /**
     * ExternalResource to which pull happens.
     */
    @ManyToOne
    private JPAExternalResource resource;

    @NotNull
    private Boolean performCreate = false;

    @NotNull
    private Boolean performUpdate = false;

    @NotNull
    private Boolean performDelete = false;

    @NotNull
    private Boolean syncStatus = false;

    /**
     * @see UnmatchingRule
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    protected UnmatchingRule unmatchingRule;

    /**
     * @see MatchingRule
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    protected MatchingRule matchingRule;

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        this.resource = (JPAExternalResource) resource;
    }

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
}
