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

import java.time.OffsetDateTime;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.persistence.jpa.validation.entity.SchedTaskCheck;

@MappedSuperclass
@SchedTaskCheck
public abstract class AbstractSchedTask<T extends Task<T>> extends AbstractTask<T> {

    private static final long serialVersionUID = 7596236684832602180L;

    @OneToOne(optional = false)
    private JPAImplementation jobDelegate;

    private OffsetDateTime startAt;

    private String cronExpression;

    @NotNull
    private String name;

    private String description;

    @NotNull
    private Boolean active = true;

    public Implementation getJobDelegate() {
        return jobDelegate;
    }

    public void setJobDelegate(final Implementation jobDelegate) {
        checkType(jobDelegate, JPAImplementation.class);
        checkImplementationType(jobDelegate, IdRepoImplementationType.TASKJOB_DELEGATE);
        this.jobDelegate = (JPAImplementation) jobDelegate;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(final OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }
}
