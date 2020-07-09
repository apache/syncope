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

import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.jpa.validation.entity.SchedTaskCheck;

@Entity
@Table(name = JPASchedTask.TABLE)
@SchedTaskCheck
public class JPASchedTask extends AbstractSchedTask implements SchedTask {

    private static final long serialVersionUID = 7596236684832602180L;

    public static final String TABLE = "SchedTask";

//    @Temporal(TemporalType.TIMESTAMP)
//    private Date startAt;
//
//    private String cronExpression;
//
//    @OneToOne(optional = false)
//    private JPAImplementation jobDelegate;
//
//    @NotNull
//    private String name;
//
//    private String description;
//
//    @NotNull
//    private Boolean active = true;
//
//    @Override
//    public Date getStartAt() {
//        if (startAt != null) {
//            return new Date(startAt.getTime());
//        }
//        return null;
//    }
//
//    @Override
//    public void setStartAt(final Date start) {
//        if (start != null) {
//            this.startAt = new Date(start.getTime());
//        } else {
//            this.startAt = null;
//        }
//    }
//
//    @Override
//    public String getCronExpression() {
//        return cronExpression;
//    }
//
//    @Override
//    public void setCronExpression(final String cronExpression) {
//        this.cronExpression = cronExpression;
//    }
//
//    @Override
//    public Implementation getJobDelegate() {
//        return jobDelegate;
//    }
//
//    @Override
//    public void setJobDelegate(final Implementation jobDelegate) {
//        checkType(jobDelegate, JPAImplementation.class);
//        checkImplementationType(jobDelegate, ImplementationType.TASKJOB_DELEGATE);
//        this.jobDelegate = (JPAImplementation) jobDelegate;
//    }
//
//    @Override
//    public String getDescription() {
//        return description;
//    }
//
//    @Override
//    public void setDescription(final String description) {
//        this.description = description;
//    }
//
//    @Override
//    public String getName() {
//        return name;
//    }
//
//    @Override
//    public void setName(final String name) {
//        this.name = name;
//    }
//
//    @Override
//    public boolean isActive() {
//        return active;
//    }
//
//    @Override
//    public void setActive(final boolean active) {
//        this.active = active;
//    }
}
