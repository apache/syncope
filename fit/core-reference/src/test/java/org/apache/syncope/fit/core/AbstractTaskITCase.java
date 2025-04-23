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
package org.apache.syncope.fit.core;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.fit.AbstractITCase;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public abstract class AbstractTaskITCase extends AbstractITCase {

    protected static final String PULL_TASK_KEY = "c41b9b71-9bfa-4f90-89f2-84787def4c5c";

    protected static final String SCHED_TASK_KEY = "e95555d2-1b09-42c8-b25b-f4c4ec597979";

    /**
     * Clean Syncope and LDAP resource status.
     */
    protected void ldapCleanup() {
        GROUP_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo("testLDAPGroup").query()).
                build()).getResult().forEach(group -> {
                    GROUP_SERVICE.deassociate(new ResourceDR.Builder().key(group.getKey()).
                            action(ResourceDeassociationAction.UNLINK).resource(RESOURCE_NAME_LDAP).build());
                    GROUP_SERVICE.delete(group.getKey());
                });
        USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("pullFromLDAP").query()).
                build()).getResult().forEach(user -> {
                    USER_SERVICE.deassociate(new ResourceDR.Builder().key(user.getKey()).
                            action(ResourceDeassociationAction.UNLINK).resource(RESOURCE_NAME_LDAP).build());
                    USER_SERVICE.delete(user.getKey());
                });
    }

    protected static ExecTO execTask(
            final TaskService taskService,
            final TaskType type,
            final String taskKey,
            final String initialStatus,
            final int maxWaitSeconds,
            final boolean dryRun) {

        Mutable<TaskTO> taskTO = new MutableObject<>(taskService.read(type, taskKey, true));
        int preSyncSize = taskTO.getValue().getExecutions().size();
        ExecTO execution = taskService.execute(new ExecSpecs.Builder().key(taskKey).dryRun(dryRun).build());
        Optional.ofNullable(initialStatus).ifPresent(status -> assertEquals(status, execution.getStatus()));
        assertNotNull(execution.getExecutor());

        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                taskTO.setValue(taskService.read(type, taskKey, true));
                return preSyncSize < taskTO.getValue().getExecutions().size();
            } catch (Exception e) {
                return false;
            }
        });

        return taskTO.getValue().getExecutions().stream().max(Comparator.comparing(ExecTO::getStart)).orElseThrow();
    }

    public static ExecTO execSchedTask(
            final TaskService taskService, final TaskType type, final String taskKey,
            final int maxWaitSeconds, final boolean dryRun) {

        return execTask(taskService, type, taskKey, "JOB_FIRED", maxWaitSeconds, dryRun);
    }

    protected static ExecTO execNotificationTask(
            final TaskService taskService, final String taskKey, final int maxWaitSeconds) {

        return execTask(taskService, TaskType.NOTIFICATION, taskKey, null, maxWaitSeconds, false);
    }

    protected void execProvisioningTasks(
            final TaskService taskService,
            final TaskType type,
            final Set<String> taskKeys,
            final int maxWaitSeconds,
            final boolean dryRun) throws Exception {

        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        List<Future<ExecTO>> futures = new ArrayList<>();

        taskKeys.forEach(taskKey -> {
            futures.add(executor.submit(() -> {
                try {
                    return execSchedTask(taskService, type, taskKey, maxWaitSeconds, dryRun);
                } catch (Exception e) {
                    ExecTO failure = new ExecTO();
                    failure.setRefKey(taskKey);
                    failure.setStatus(ExecStatus.FAILURE.name());
                    failure.setMessage(e.getMessage());
                    return failure;
                }
            }));

            // avoid flooding the test server
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // ignore
            }
        });

        futures.forEach(future -> {
            try {
                future.get(maxWaitSeconds, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOG.error("While getting futures", e);
            }
        });
    }

    protected NotificationTaskTO findNotificationTask(final String notification, final int maxWaitSeconds) {
        Mutable<NotificationTaskTO> notificationTask = new MutableObject<>();
        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                PagedResult<NotificationTaskTO> tasks = TASK_SERVICE.search(
                        new TaskQuery.Builder(TaskType.NOTIFICATION).notification(notification).build());
                if (!tasks.getResult().isEmpty()) {
                    notificationTask.setValue(tasks.getResult().getFirst());
                }
            } catch (Exception e) {
                // ignore
            }
            return notificationTask.getValue() != null;
        });

        return notificationTask.getValue();
    }
}
