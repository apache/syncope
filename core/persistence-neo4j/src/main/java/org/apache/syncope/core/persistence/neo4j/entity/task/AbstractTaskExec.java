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

import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractExec;

public abstract class AbstractTaskExec<T extends Task<T>> extends AbstractExec implements TaskExec<T> {

    private static final long serialVersionUID = 1909033231464074554L;

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('{').
                append("id=").append(getKey()).append(", ").
                append("start=").append(start).append(", ").
                append("end=").append(end).append(", ").
                append("task=").append(getTask()).append(", ").
                append("status=").append(status).append(", ").
                append("message=").append(message).
                append('}').
                toString();
    }
}
