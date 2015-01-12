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
package org.apache.syncope.server.persistence.jpa.entity.task;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.server.persistence.api.entity.task.PushTask;
import org.apache.syncope.server.provisioning.api.job.PushJob;

@Entity
@DiscriminatorValue("PushTask")
public class JPAPushTask extends AbstractProvisioningTask implements PushTask {

    private static final long serialVersionUID = -4141057723006682564L;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "actionClassName")
    @CollectionTable(name = "PushTask_actionsClassNames",
            joinColumns =
            @JoinColumn(name = "PushTask_id", referencedColumnName = "id"))
    private List<String> actionsClassNames = new ArrayList<>();

    private String userFilter;

    private String roleFilter;

    /**
     * Default constructor.
     */
    public JPAPushTask() {
        super(TaskType.PUSH, PushJob.class.getName());
    }

    @Override
    public List<String> getActionsClassNames() {
        return actionsClassNames;
    }

    @Override
    public String getUserFilter() {
        return userFilter;
    }

    @Override
    public void setUserFilter(final String filter) {
        this.userFilter = filter;
    }

    @Override
    public String getRoleFilter() {
        return roleFilter;
    }

    @Override
    public void setRoleFilter(final String roleFilter) {
        this.roleFilter = roleFilter;
    }
}
