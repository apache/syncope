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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.common.validation.SchedTaskCheck;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jSchedTask.NODE)
@SchedTaskCheck
public class Neo4jSchedTask extends AbstractTask<SchedTask> implements SchedTask {

    private static final long serialVersionUID = 7596236684832602180L;

    public static final String NODE = "SchedTask";

    public static final String SCHED_TASK_JOB_DELEGATE_REL = "SCHED_TASK_JOB_DELEGATE";

    public static final String SCHED_TASK_EXEC_REL = "SCHED_TASK_EXEC";

    @NotNull
    @Relationship(type = SCHED_TASK_JOB_DELEGATE_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jImplementation jobDelegate;

    private OffsetDateTime startAt;

    private String cronExpression;

    @NotNull
    private String name;

    private String description;

    @NotNull
    private Boolean active = true;

    @Relationship(type = SCHED_TASK_EXEC_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jSchedTaskExec> executions = new ArrayList<>();

    @Override
    public Implementation getJobDelegate() {
        return jobDelegate;
    }

    @Override
    public void setJobDelegate(final Implementation jobDelegate) {
        checkType(jobDelegate, Neo4jImplementation.class);
        checkImplementationType(jobDelegate, IdRepoImplementationType.TASKJOB_DELEGATE);
        this.jobDelegate = (Neo4jImplementation) jobDelegate;
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
    protected boolean doAdd(final TaskExec<SchedTask> exec) {
        return executions.add((Neo4jSchedTaskExec) exec);
    }

    @Override
    protected Class<? extends AbstractTaskExec<SchedTask>> executionClass() {
        return Neo4jSchedTaskExec.class;
    }

    @Override
    protected List<? extends AbstractTaskExec<SchedTask>> executions() {
        return executions;
    }
}
