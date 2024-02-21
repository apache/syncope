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
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jNotificationTaskExec.NODE)
public class Neo4jNotificationTaskExec
        extends AbstractTaskExec<NotificationTask>
        implements TaskExec<NotificationTask> {

    private static final long serialVersionUID = 1909033231464074554L;

    public static final String NODE = "NotificationTaskExec";

    @NotNull
    @Relationship(type = Neo4jNotificationTask.NOTIFICATION_TASK_EXEC_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jNotificationTask task;

    @Override
    public NotificationTask getTask() {
        return task;
    }

    @Override
    public void setTask(final NotificationTask task) {
        checkType(task, NotificationTask.class);
        this.task = (Neo4jNotificationTask) task;
    }
}
