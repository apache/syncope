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
package org.syncope.core.persistence.beans;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.validation.entity.SyncTaskCheck;
import org.syncope.core.scheduling.SyncJob;
import org.syncope.client.util.XMLSerializer;

@Entity
@SyncTaskCheck
public class SyncTask extends SchedTask {

    private static final long serialVersionUID = -4141057723006682562L;

    /**
     * ExternalResource to which the sync happens.
     */
    @ManyToOne
    private ExternalResource resource;

    @Lob
    private String userTemplate;

    @Basic
    @Min(0)
    @Max(1)
    private Integer performCreate;

    @Basic
    @Min(0)
    @Max(1)
    private Integer performUpdate;

    @Basic
    @Min(0)
    @Max(1)
    private Integer performDelete;

    @Basic
    @Min(0)
    @Max(1)
    private Integer syncStatus;

    @Basic
    @Min(0)
    @Max(1)
    private Integer fullReconciliation;

    private String jobActionsClassName;

    /**
     * Default constructor.
     */
    public SyncTask() {
        super();

        super.setJobClassName(SyncJob.class.getName());
    }

    @Override
    public void setJobClassName(String jobClassName) {
    }

    public ExternalResource getResource() {
        return resource;
    }

    public void setResource(ExternalResource resource) {
        this.resource = resource;
    }

    public UserTO getUserTemplate() {
        return userTemplate == null
                ? new UserTO()
                : XMLSerializer.<UserTO>deserialize(userTemplate);
    }

    public void setUserTemplate(final UserTO userTemplate) {
        this.userTemplate = XMLSerializer.serialize(userTemplate);
    }

    public boolean isPerformCreate() {
        return isBooleanAsInteger(performCreate);
    }

    public void setPerformCreate(final boolean performCreate) {
        this.performCreate = getBooleanAsInteger(performCreate);
    }

    public boolean isPerformUpdate() {
        return isBooleanAsInteger(performUpdate);
    }

    public void setPerformUpdate(final boolean performUpdate) {
        this.performUpdate = getBooleanAsInteger(performUpdate);
    }

    public boolean isPerformDelete() {
        return isBooleanAsInteger(performDelete);
    }

    public void setPerformDelete(boolean performDelete) {
        this.performDelete = getBooleanAsInteger(performDelete);
    }

    public boolean isSyncStatus() {
        return isBooleanAsInteger(syncStatus);
    }

    public void setSyncStatus(final boolean syncStatus) {
        this.syncStatus = getBooleanAsInteger(syncStatus);
    }

    public boolean isFullReconciliation() {
        return isBooleanAsInteger(fullReconciliation);
    }

    public void setFullReconciliation(final boolean fullReconciliation) {
        this.fullReconciliation = getBooleanAsInteger(fullReconciliation);
    }

    public String getJobActionsClassName() {
        return jobActionsClassName;
    }

    public void setJobActionsClassName(String jobActionsClassName) {
        this.jobActionsClassName = jobActionsClassName;
    }
}
