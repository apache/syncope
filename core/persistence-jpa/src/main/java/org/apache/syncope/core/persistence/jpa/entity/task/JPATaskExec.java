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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.jpa.entity.AbstractExec;

/**
 * An execution (with result) of a Task.
 *
 * @see AbstractTask
 */
@Entity
@Table(name = JPATaskExec.TABLE)
public class JPATaskExec extends AbstractExec implements TaskExec {

    private static final long serialVersionUID = 1909033231464074554L;

    public static final String TABLE = "TaskExec";

    /**
     * The referred task.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private AbstractTask task;

    @Override
    public Task getTask() {
        return task;
    }

    @Override
    public void setTask(final Task task) {
        checkType(task, AbstractTask.class);
        this.task = (AbstractTask) task;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('{').
                append("id=").append(getKey()).append(", ").
                append("start=").append(start).append(", ").
                append("end=").append(end).append(", ").
                append("task=").append(task).append(", ").
                append("status=").append(status).append(", ").
                append("message=").append(message).
                append('}').
                toString();
    }
}
