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

import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.jpa.validation.entity.SchedTaskCheck;

@Entity
@DiscriminatorValue("SchedTask")
@SchedTaskCheck
public class JPASchedTask extends AbstractTask implements SchedTask {

    private static final long serialVersionUID = 7596236684832602180L;

    @Temporal(TemporalType.TIMESTAMP)
    private Date startAt;

    private String cronExpression;

    private String jobDelegateClassName;

    @NotNull
    private String name;

    private String description;

    @NotNull
    @Basic
    @Min(0)
    @Max(1)
    private Integer active;

    @Override
    public Date getStartAt() {
        if (startAt != null) {
            return new Date(startAt.getTime());
        }
        return null;
    }

    @Override
    public void setStartAt(final Date start) {
        if (start != null) {
            this.startAt = new Date(start.getTime());
        } else {
            this.startAt = null;
        }
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
    public String getJobDelegateClassName() {
        return jobDelegateClassName;
    }

    @Override
    public void setJobDelegateClassName(final String jobDelegateClassName) {
        this.jobDelegateClassName = jobDelegateClassName;
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
        return isBooleanAsInteger(active);
    }

    @Override
    public void setActive(final boolean active) {
        this.active = getBooleanAsInteger(active);
    }
}
