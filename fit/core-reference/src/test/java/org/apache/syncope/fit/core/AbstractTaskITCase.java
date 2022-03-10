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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.apache.syncope.fit.AbstractITCase;

public abstract class AbstractTaskITCase extends AbstractITCase {

    protected static final String PULL_TASK_KEY = "c41b9b71-9bfa-4f90-89f2-84787def4c5c";

    protected static final String SCHED_TASK_KEY = "e95555d2-1b09-42c8-b25b-f4c4ec597979";

    /**
     * Remove initial and synchronized users to make test re-runnable.
     */
    protected void removeTestUsers() {
        for (int i = 0; i < 10; i++) {
            String cUserName = "test" + i;
            try {
                UserTO cUserTO = userService.read(cUserName);
                userService.delete(cUserTO.getKey());
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Clean Syncope and LDAP resource status.
     */
    protected void ldapCleanup() {
        groupService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo("testLDAPGroup").query()).
                build()).getResult().forEach(group -> {
                    groupService.deassociate(new ResourceDR.Builder().key(group.getKey()).
                            action(ResourceDeassociationAction.UNLINK).resource(RESOURCE_NAME_LDAP).build());
                    groupService.delete(group.getKey());
                });
        userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("pullFromLDAP").query()).
                build()).getResult().forEach(user -> {
                    userService.deassociate(new ResourceDR.Builder().key(user.getKey()).
                            action(ResourceDeassociationAction.UNLINK).resource(RESOURCE_NAME_LDAP).build());
                    userService.delete(user.getKey());
                });
    }

    protected static ExecTO execTask(
            final TaskService taskService,
            final TaskType type,
            final String taskKey,
            final String initialStatus,
            final int maxWaitSeconds,
            final boolean dryRun) {

        AtomicReference<TaskTO> taskTO = new AtomicReference<>(taskService.read(type, taskKey, true));
        int preSyncSize = taskTO.get().getExecutions().size();
        ExecTO execution = taskService.execute(new ExecSpecs.Builder().key(taskKey).dryRun(dryRun).build());
        assertEquals(initialStatus, execution.getStatus());
        assertNotNull(execution.getExecutor());

        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                taskTO.set(taskService.read(type, taskKey, true));
                return preSyncSize < taskTO.get().getExecutions().size();
            } catch (Exception e) {
                return false;
            }
        });

        return taskTO.get().getExecutions().get(taskTO.get().getExecutions().size() - 1);
    }

    public static ExecTO execProvisioningTask(
            final TaskService taskService, final TaskType type, final String taskKey,
            final int maxWaitSeconds, final boolean dryRun) {

        return execTask(taskService, type, taskKey, "JOB_FIRED", maxWaitSeconds, dryRun);
    }

    protected static ExecTO execNotificationTask(
            final TaskService taskService, final String taskKey, final int maxWaitSeconds) {

        return execTask(taskService, TaskType.NOTIFICATION, taskKey,
                NotificationJob.Status.SENT.name(), maxWaitSeconds, false);
    }

    protected void execProvisioningTasks(
            final TaskService taskService,
            final TaskType type,
            final Set<String> taskKeys,
            final int maxWaitSeconds,
            final boolean dryRun) throws Exception {

        ExecutorService service = Executors.newFixedThreadPool(taskKeys.size());
        List<Future<ExecTO>> futures = new ArrayList<>();

        taskKeys.forEach(taskKey -> {
            futures.add(service.submit(() -> {
                try {
                    return execProvisioningTask(taskService, type, taskKey, maxWaitSeconds, dryRun);
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

        service.shutdownNow();
    }

    protected NotificationTaskTO findNotificationTask(final String notification, final int maxWaitSeconds) {
        AtomicReference<NotificationTaskTO> notificationTask = new AtomicReference<>();
        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                PagedResult<NotificationTaskTO> tasks = taskService.search(
                        new TaskQuery.Builder(TaskType.NOTIFICATION).notification(notification).build());
                if (!tasks.getResult().isEmpty()) {
                    notificationTask.set(tasks.getResult().get(0));
                }
            } catch (Exception e) {
                // ignore
            }
            return notificationTask.get() != null;
        });

        return notificationTask.get();
    }
}
