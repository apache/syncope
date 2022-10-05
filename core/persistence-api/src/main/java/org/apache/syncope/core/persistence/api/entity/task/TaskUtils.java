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
package org.apache.syncope.core.persistence.api.entity.task;

import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.TaskType;

public interface TaskUtils {

    TaskType getType();

    <T extends Task<T>> T newTask();

    <E extends TaskExec<?>> E newTaskExec();

    <T extends TaskTO> T newTaskTO();

    <T extends Task<T>> Class<T> taskClass();

    <T extends TaskTO> Class<T> taskTOClass();

    String getTaskTable();

    Class<? extends Task<?>> getTaskEntity();

    String getTaskExecTable();

    Class<? extends TaskExec<?>> getTaskExecEntity();
}
