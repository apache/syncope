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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.common.validation.SchedTaskCheck;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;

@Entity
@Table(name = JPASchedTask.TABLE)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@SchedTaskCheck
public class JPASchedTask extends AbstractTask<SchedTask> implements SchedTask {

    private static final long serialVersionUID = 7596236684832602180L;

    public static final String TABLE = "SchedTask";

    @OneToOne(optional = false)
    private JPAImplementation jobDelegate;

    private OffsetDateTime startAt;

    private String cronExpression;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @NotNull
    private Boolean active = true;

    @OneToMany(targetEntity = JPASchedTaskExec.class,
            cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "task")
    private List<TaskExec<SchedTask>> executions = new ArrayList<>();

    @Override
    public Implementation getJobDelegate() {
        return jobDelegate;
    }

    @Override
    public void setJobDelegate(final Implementation jobDelegate) {
        checkType(jobDelegate, JPAImplementation.class);
        checkImplementationType(jobDelegate, IdRepoImplementationType.TASKJOB_DELEGATE);
        this.jobDelegate = (JPAImplementation) jobDelegate;
    }

    @Override
    public OffsetDateTime getStartAt() {
        return startAt;
    }

    @Override
    public void setStartAt(final OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    @Override
    public String getCronExpression() {
        return cronExpression;
    }

    @Override
    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    protected Class<? extends TaskExec<SchedTask>> executionClass() {
        return JPASchedTaskExec.class;
    }

    @Override
    protected List<TaskExec<SchedTask>> executions() {
        return executions;
    }
}
