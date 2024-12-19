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
import java.util.Optional;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.springframework.data.domain.Pageable;

public interface TaskDAO extends DAO<Task<?>> {

    <T extends Task<T>> Optional<T> findById(TaskType type, String key);

    <T extends SchedTask> Optional<T> findByName(TaskType type, String name);

    List<SchedTask> findByDelegate(Implementation delegate);

    List<PullTask> findByReconFilterBuilder(Implementation reconFilterBuilder);

    List<PullTask> findByInboundActions(Implementation inboundActions);

    List<PushTask> findByPushActions(Implementation pushActions);

    List<MacroTask> findByCommand(Implementation delegate);

    List<MacroTask> findByRealm(Realm realm);

    <T extends Task<T>> List<T> findToExec(TaskType type);

    <T extends Task<T>> List<T> findAll(TaskType type);

    <T extends Task<T>> List<T> findAll(
            TaskType type,
            ExternalResource resource,
            Notification notification,
            AnyTypeKind anyTypeKind,
            String entityKey,
            Pageable pageable);

    long count(
            TaskType type,
            ExternalResource resource,
            Notification notification,
            AnyTypeKind anyTypeKind,
            String entityKey);

    void delete(TaskType type, String key);

    void deleteAll(ExternalResource resource, TaskType type);

    List<PropagationTaskTO> purgePropagations(
            OffsetDateTime since,
            List<ExecStatus> statuses,
            List<String> resources);
}
