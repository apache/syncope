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

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;

@Entity
@Table(name = JPAPropagationTaskExec.TABLE)
public class JPAPropagationTaskExec extends AbstractTaskExec<PropagationTask> implements TaskExec<PropagationTask> {

    private static final long serialVersionUID = 1909033231464074554L;

    public static final String TABLE = "PropagationTaskExec";

    @ManyToOne(optional = false)
    private JPAPropagationTask task;

    @Override
    public PropagationTask getTask() {
        return task;
    }

    @Override
    public void setTask(final PropagationTask task) {
        checkType(task, PropagationTask.class);
        this.task = (JPAPropagationTask) task;
    }
}
