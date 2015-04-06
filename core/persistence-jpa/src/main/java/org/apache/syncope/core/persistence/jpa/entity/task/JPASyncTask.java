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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.provisioning.api.job.SyncJob;
import org.apache.syncope.core.misc.serialization.POJOHelper;

@Entity
@DiscriminatorValue("SyncTask")
public class JPASyncTask extends AbstractProvisioningTask implements SyncTask {

    private static final long serialVersionUID = -4141057723006682563L;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "actionClassName")
    @CollectionTable(name = "SyncTask_actionsClassNames",
            joinColumns =
            @JoinColumn(name = "SyncTask_id", referencedColumnName = "id"))
    private List<String> actionsClassNames = new ArrayList<>();

    @Lob
    private String userTemplate;

    @Lob
    private String groupTemplate;

    @Basic
    @Min(0)
    @Max(1)
    private Integer fullReconciliation;

    /**
     * Default constructor.
     */
    public JPASyncTask() {
        super(TaskType.SYNCHRONIZATION, SyncJob.class.getName());
    }

    @Override
    public List<String> getActionsClassNames() {
        return actionsClassNames;
    }

    @Override
    public UserTO getUserTemplate() {
        return userTemplate == null
                ? new UserTO()
                : POJOHelper.deserialize(userTemplate, UserTO.class);
    }

    @Override
    public void setUserTemplate(final UserTO userTemplate) {
        this.userTemplate = POJOHelper.serialize(userTemplate);
    }

    @Override
    public GroupTO getGroupTemplate() {
        return userTemplate == null
                ? new GroupTO()
                : POJOHelper.deserialize(groupTemplate, GroupTO.class);
    }

    @Override
    public void setGroupTemplate(final GroupTO groupTemplate) {
        this.groupTemplate = POJOHelper.serialize(groupTemplate);
    }

    @Override
    public boolean isFullReconciliation() {
        return isBooleanAsInteger(fullReconciliation);
    }

    @Override
    public void setFullReconciliation(final boolean fullReconciliation) {
        this.fullReconciliation = getBooleanAsInteger(fullReconciliation);
    }
}
