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
package org.apache.syncope.fit.core.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.logic.notification.NotificationJob;

public abstract class AbstractTaskITCase extends AbstractITCase {

    protected static final Long SYNC_TASK_ID = 4L;

    protected static final Long SCHED_TASK_ID = 5L;

    protected static class ThreadExec implements Callable<TaskExecTO> {

        private final TaskService taskService;

        private final Long taskKey;

        private final int maxWaitSeconds;

        private final boolean dryRun;

        public ThreadExec(
                final TaskService taskService, final Long taskKey, final int maxWaitSeconds, final boolean dryRun) {

            this.taskService = taskService;
            this.taskKey = taskKey;
            this.maxWaitSeconds = maxWaitSeconds;
            this.dryRun = dryRun;
        }

        @Override
        public TaskExecTO call() throws Exception {
            return execProvisioningTask(taskService, taskKey, maxWaitSeconds, dryRun);
        }
    }

    /**
     * Remove initial and synchronized users to make test re-runnable.
     */
    protected void removeTestUsers() {
        for (int i = 0; i < 10; i++) {
            String cUserName = "test" + i;
            try {
                UserTO cUserTO = readUser(cUserName);
                userService.delete(cUserTO.getKey());
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    protected static TaskExecTO execTask(final TaskService taskService, final Long taskKey, final String initialStatus,
            final int maxWaitSeconds, final boolean dryRun) {

        AbstractTaskTO taskTO = taskService.read(taskKey, true);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());

        int preSyncSize = taskTO.getExecutions().size();
        TaskExecTO execution = taskService.execute(
                new ExecuteQuery.Builder().key(taskTO.getKey()).dryRun(dryRun).build());
        assertEquals(initialStatus, execution.getStatus());

        int i = 0;
        int maxit = maxWaitSeconds;

        // wait for completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            taskTO = taskService.read(taskTO.getKey(), true);

            assertNotNull(taskTO);
            assertNotNull(taskTO.getExecutions());

            i++;
        } while (preSyncSize == taskTO.getExecutions().size() && i < maxit);
        if (i == maxit) {
            fail("Timeout when executing task " + taskKey);
        }
        return taskTO.getExecutions().get(taskTO.getExecutions().size() - 1);
    }

    public static TaskExecTO execProvisioningTask(
            final TaskService taskService, final Long taskKey, final int maxWaitSeconds, final boolean dryRun) {

        return execTask(taskService, taskKey, "JOB_FIRED", maxWaitSeconds, dryRun);
    }

    protected static TaskExecTO execNotificationTask(
            final TaskService taskService, final Long taskKey, final int maxWaitSeconds) {

        return execTask(taskService, taskKey, NotificationJob.Status.SENT.name(), maxWaitSeconds, false);
    }

    protected Map<Long, TaskExecTO> execProvisioningTasks(final TaskService taskService,
            final Set<Long> taskKeys, final int maxWaitSeconds, final boolean dryRun) throws Exception {

        ExecutorService service = Executors.newFixedThreadPool(taskKeys.size());
        List<Future<TaskExecTO>> futures = new ArrayList<>();

        for (Long key : taskKeys) {
            futures.add(service.submit(new ThreadExec(taskService, key, maxWaitSeconds, dryRun)));
            // avoid flooding the test server
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }

        Map<Long, TaskExecTO> res = new HashMap<>();

        for (Future<TaskExecTO> future : futures) {
            TaskExecTO taskExecTO = future.get(100, TimeUnit.SECONDS);
            res.put(taskExecTO.getTask(), taskExecTO);
        }

        service.shutdownNow();

        return res;
    }

    protected NotificationTaskTO findNotificationTaskBySender(final String sender) {
        PagedResult<NotificationTaskTO> tasks =
                taskService.list(new TaskQuery.Builder().type(TaskType.NOTIFICATION).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        return IterableUtils.find(tasks.getResult(), new Predicate<NotificationTaskTO>() {

            @Override
            public boolean evaluate(final NotificationTaskTO task) {
                return sender.equals(task.getSender());
            }
        });
    }
}
