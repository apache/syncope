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
package org.apache.syncope.core.persistence.api.dao;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;

public interface TaskExecDAO extends DAO<TaskExec> {

    TaskExec find(String key);

    List<TaskExec> findRecent(int max);

    <T extends Task> TaskExec findLatestStarted(T task);

    <T extends Task> TaskExec findLatestEnded(T task);

    int count(String taskKey);

    <T extends Task> List<TaskExec> findAll(T task, int page, int itemsPerPage, List<OrderByClause> orderByClauses);

    <T extends Task> List<TaskExec> findAll(
            T task,
            OffsetDateTime startedBefore,
            OffsetDateTime startedAfter,
            OffsetDateTime endedBefore,
            OffsetDateTime endedAfter);

    TaskExec save(TaskExec execution);

    void saveAndAdd(String taskKey, TaskExec execution);

    void delete(String key);

    void delete(TaskExec execution);
}
