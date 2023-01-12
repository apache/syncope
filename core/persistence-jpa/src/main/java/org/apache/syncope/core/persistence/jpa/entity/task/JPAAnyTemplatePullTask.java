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
import jakarta.persistence.UniqueConstraint;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.jpa.entity.AbstractAnyTemplate;

@Entity
@Table(name = JPAAnyTemplatePullTask.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "pullTask_id", "anyType_id" }))
public class JPAAnyTemplatePullTask extends AbstractAnyTemplate implements AnyTemplatePullTask {

    private static final long serialVersionUID = 3517381731849788407L;

    public static final String TABLE = "AnyTemplatePullTask";

    @ManyToOne
    private JPAPullTask pullTask;

    @Override
    public PullTask getPullTask() {
        return pullTask;
    }

    @Override
    public void setPullTask(final PullTask pullTask) {
        checkType(pullTask, JPAPullTask.class);
        this.pullTask = (JPAPullTask) pullTask;
    }
}
