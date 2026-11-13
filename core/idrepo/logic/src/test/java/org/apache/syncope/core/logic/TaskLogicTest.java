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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.notification.NotificationJobDelegate;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class TaskLogicTest {

    private static final String UUID = "11111111-1111-1111-1111-111111111111";

    private JobManager jobManager;

    private SyncopeTaskScheduler scheduler;

    private JobStatusDAO jobStatusDAO;

    private TaskDAO taskDAO;

    private TaskExecDAO taskExecDAO;

    private ExternalResourceDAO resourceDAO;

    private NotificationDAO notificationDAO;

    private TaskDataBinder binder;

    private PropagationTaskExecutor taskExecutor;

    private NotificationJobDelegate notificationJobDelegate;

    private TaskUtilsFactory taskUtilsFactory;

    private TaskLogic logic;

    @BeforeEach
    void setUp() {
        jobManager = mock(JobManager.class);
        scheduler = mock(SyncopeTaskScheduler.class);
        jobStatusDAO = mock(JobStatusDAO.class);
        taskDAO = mock(TaskDAO.class);
        taskExecDAO = mock(TaskExecDAO.class);
        resourceDAO = mock(ExternalResourceDAO.class);
        notificationDAO = mock(NotificationDAO.class);
        binder = mock(TaskDataBinder.class);
        taskExecutor = mock(PropagationTaskExecutor.class);
        notificationJobDelegate = mock(NotificationJobDelegate.class);
        taskUtilsFactory = mock(TaskUtilsFactory.class);

        logic = new TaskLogic(
                jobManager,
                scheduler,
                jobStatusDAO,
                taskDAO,
                taskExecDAO,
                resourceDAO,
                notificationDAO,
                binder,
                taskExecutor,
                notificationJobDelegate,
                taskUtilsFactory);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void searchMissingResource() {
        doReturn(Optional.empty()).when(resourceDAO).findById("missing");

        assertThrows(SyncopeClientException.class, () -> logic.search(
                TaskType.PROPAGATION, "missing", null, null, null, Pageable.unpaged(), false));
    }

    @Test
    void search() {
        Pageable pageable = Pageable.unpaged();
        when(taskDAO.count(TaskType.PROPAGATION, null, null, null, null)).thenReturn(0L);
        when(taskDAO.findAll(
                TaskType.PROPAGATION, null, null, null, null, pageable)).thenReturn(List.of());

        assertEquals(0, logic.search(
                TaskType.PROPAGATION, null, null, null, null, pageable, false).getTotalElements());
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(taskDAO).findById(TaskType.PROPAGATION, "missing");

        assertThrows(NotFoundException.class, () -> logic.read(TaskType.PROPAGATION, "missing", false));
    }

    @Test
    void readWithWrongType() {
        PropagationTask task = mock(PropagationTask.class);
        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.NOTIFICATION);
        doReturn(Optional.of(task)).when(taskDAO).findById(TaskType.PROPAGATION, "key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);

        assertThrows(SyncopeClientException.class, () -> logic.read(TaskType.PROPAGATION, "key", false));
    }

    @Test
    void readTask() {
        PropagationTask task = mock(PropagationTask.class);
        TaskUtils taskUtils = mock(TaskUtils.class);
        PropagationTaskTO to = new PropagationTaskTO();
        to.setKey("key");
        when(taskUtils.getType()).thenReturn(TaskType.PROPAGATION);
        doReturn(Optional.of(task)).when(taskDAO).findById(TaskType.PROPAGATION, "key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);
        doReturn(to).when(binder).getTaskTO(task, taskUtils, false);

        assertSame(to, logic.read(TaskType.PROPAGATION, "key", false));
    }

    @Test
    void readMacroTaskWithoutAuthorization() {
        MacroTask task = mock(MacroTask.class);
        Realm realm = mock(Realm.class);
        when(realm.getFullPath()).thenReturn("/secret");
        when(task.getRealm()).thenReturn(realm);

        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.MACRO);
        doReturn(Optional.of(task)).when(taskDAO).findById(TaskType.MACRO, "key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);

        assertThrows(DelegatedAdministrationException.class,
                () -> logic.read(TaskType.MACRO, "key", false));
    }

    @Test
    void readMacroTask() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdRepoEntitlement.TASK_READ, Set.of(SyncopeConstants.ROOT_REALM)));

        MacroTask task = mock(MacroTask.class);
        Realm realm = mock(Realm.class);
        when(realm.getFullPath()).thenReturn(SyncopeConstants.ROOT_REALM);
        when(task.getRealm()).thenReturn(realm);

        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.MACRO);
        MacroTaskTO to = new MacroTaskTO();
        to.setKey("key");

        doReturn(Optional.of(task)).when(taskDAO).findById(TaskType.MACRO, "key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);
        doReturn(to).when(binder).getTaskTO(task, taskUtils, false);

        assertSame(to, logic.read(TaskType.MACRO, "key", false));
    }

    @Test
    void createSchedTaskWithWrongType() {
        SchedTaskTO to = new SchedTaskTO();
        to.setKey("key");

        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.PUSH);
        when(taskUtilsFactory.getInstance(to)).thenReturn(taskUtils);

        assertThrows(SyncopeClientException.class, () -> logic.createSchedTask(TaskType.SCHEDULED, to));
    }

    @Test
    void createSchedTask() {
        SchedTaskTO to = new SchedTaskTO();
        to.setKey("key");

        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.SCHEDULED);
        when(taskUtilsFactory.getInstance(to)).thenReturn(taskUtils);

        SchedTask task = mock(SchedTask.class);
        when(task.getKey()).thenReturn("key");
        when(binder.createSchedTask(to, taskUtils)).thenReturn(task);
        when(taskDAO.save(task)).thenReturn(task);
        doReturn(to).when(binder).getTaskTO(task, taskUtils, false);

        assertSame(to, logic.createSchedTask(TaskType.SCHEDULED, to));
        verify(jobManager).register(task, "unauthenticated");
    }

    @Test
    void updateSchedTaskMissing() {
        SchedTaskTO to = new SchedTaskTO();
        to.setKey("missing");
        doReturn(Optional.empty()).when(taskDAO).findById(TaskType.SCHEDULED, "missing");

        assertThrows(NotFoundException.class, () -> logic.updateSchedTask(TaskType.SCHEDULED, to));
    }

    @Test
    void updateSchedTask() {
        SchedTaskTO to = new SchedTaskTO();
        to.setKey("key");

        SchedTask task = mock(SchedTask.class);
        when(task.isActive()).thenReturn(false);
        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.SCHEDULED);
        doReturn(Optional.of(task)).when(taskDAO).findById(TaskType.SCHEDULED, "key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);
        when(taskDAO.save(task)).thenReturn(task);
        doReturn(to).when(binder).getTaskTO(task, taskUtils, false);

        assertSame(to, logic.updateSchedTask(TaskType.SCHEDULED, to));
        verify(jobManager).unregister(task);
    }

    @Test
    void executeMissing() {
        doReturn(Optional.empty()).when(taskDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.execute(new ExecSpecs.Builder().key("missing").build()));
    }

    @Test
    void executeInThePast() {
        SchedTask task = mock(SchedTask.class);
        doReturn(Optional.of(task)).when(taskDAO).findById("key");

        ExecSpecs specs = new ExecSpecs.Builder().key("key").
                startAt(OffsetDateTime.now().minusDays(1)).build();

        assertThrows(SyncopeClientException.class, () -> logic.execute(specs));
    }

    @Test
    void executePropagation() {
        PropagationTask task = mock(PropagationTask.class);
        when(task.getKey()).thenReturn("key");
        when(task.getObjectClassName()).thenReturn("__ACCOUNT__");
        when(task.getResource()).thenReturn(mock(ExternalResource.class));
        when(task.getOperation()).thenReturn(ResourceOperation.CREATE);
        when(task.getAnyTypeKind()).thenReturn(AnyTypeKind.USER);
        when(task.getConnObjectKey()).thenReturn("connObjectKey");

        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.PROPAGATION);
        doReturn(Optional.of(task)).when(taskDAO).findById("key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);

        @SuppressWarnings("unchecked")
        TaskExec<PropagationTask> exec = mock(TaskExec.class);
        ExecTO execTO = new ExecTO();
        when(taskExecutor.execute(any(), any(), eq("unauthenticated"))).thenReturn(exec);
        when(binder.getExecTO(exec)).thenReturn(execTO);

        assertSame(execTO, logic.execute(new ExecSpecs.Builder().key("key").build()));
    }

    @Test
    void executeNotification() {
        NotificationTask task = mock(NotificationTask.class);
        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.NOTIFICATION);
        doReturn(Optional.of(task)).when(taskDAO).findById("key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);

        @SuppressWarnings("unchecked")
        TaskExec<NotificationTask> exec = mock(TaskExec.class);
        ExecTO execTO = new ExecTO();
        when(notificationJobDelegate.executeSingle(task, "unauthenticated")).thenReturn(exec);
        when(binder.getExecTO(exec)).thenReturn(execTO);

        assertSame(execTO, logic.execute(new ExecSpecs.Builder().key("key").build()));
    }

    @Test
    void executeInactiveScheduled() {
        SchedTask task = mock(SchedTask.class);
        when(task.getKey()).thenReturn("key");
        when(task.isActive()).thenReturn(false);
        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.SCHEDULED);
        doReturn(Optional.of(task)).when(taskDAO).findById("key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);

        assertThrows(SyncopeClientException.class, () -> logic.execute(new ExecSpecs.Builder().key("key").build()));
    }

    @Test
    void executeScheduled() {
        SchedTask task = mock(SchedTask.class);
        when(task.getKey()).thenReturn("key");
        when(task.isActive()).thenReturn(true);
        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.SCHEDULED);
        doReturn(Optional.of(task)).when(taskDAO).findById("key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);
        when(binder.buildRefDesc(task)).thenReturn("desc");

        ExecTO execTO = logic.execute(new ExecSpecs.Builder().key("key").build());

        assertEquals(JobType.TASK, execTO.getJobType());
        assertEquals("key", execTO.getRefKey());
        assertEquals("desc", execTO.getRefDesc());
        assertEquals(JobStatusDAO.JOB_FIRED_STATUS, execTO.getStatus());
        verify(jobManager).execute(eq(task), any(), eq("unauthenticated"), eq(false), any());
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(taskDAO).findById(TaskType.SCHEDULED, "missing");

        assertThrows(NotFoundException.class, () -> logic.delete(TaskType.SCHEDULED, "missing"));
    }

    @Test
    void deleteScheduled() {
        SchedTask task = mock(SchedTask.class);
        TaskUtils taskUtils = mock(TaskUtils.class);
        when(taskUtils.getType()).thenReturn(TaskType.SCHEDULED);
        doReturn(Optional.of(task)).when(taskDAO).findById(TaskType.SCHEDULED, "key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);

        TaskTO to = new SchedTaskTO();
        to.setKey("key");
        doReturn(to).when(binder).getTaskTO(task, taskUtils, true);

        assertSame(to, logic.delete(TaskType.SCHEDULED, "key"));
        verify(jobManager).unregister(task);
        verify(taskDAO).delete(task);
    }

    @Test
    void listExecutions() {
        SchedTask task = mock(SchedTask.class);
        doReturn(Optional.of(task)).when(taskDAO).findById("key");

        Pageable pageable = Pageable.unpaged();
        @SuppressWarnings("unchecked")
        TaskExec<SchedTask> exec = mock(TaskExec.class);
        ExecTO execTO = new ExecTO();
        when(taskExecDAO.count(task, null, null)).thenReturn(1L);
        when(taskExecDAO.findAll(task, null, null, pageable)).thenReturn(List.of(exec));
        when(binder.getExecTO(exec)).thenReturn(execTO);

        assertEquals(1, logic.listExecutions("key", null, null, pageable).getTotalElements());
        assertSame(execTO, logic.listExecutions("key", null, null, pageable).getContent().getFirst());
    }

    @Test
    void deleteExecutionMissing() {
        doReturn(Optional.empty()).when(taskExecDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.deleteExecution("missing"));
    }

    @Test
    void deleteExecution() {
        @SuppressWarnings("unchecked")
        TaskExec<SchedTask> exec = mock(TaskExec.class);
        ExecTO execTO = new ExecTO();
        doReturn(Optional.of(exec)).when(taskExecDAO).findById("execKey");
        when(binder.getExecTO(exec)).thenReturn(execTO);

        assertSame(execTO, logic.deleteExecution("execKey"));
        verify(taskExecDAO).delete(exec);
    }

    @Test
    void deleteExecutions() {
        SchedTask task = mock(SchedTask.class);
        doReturn(Optional.of(task)).when(taskDAO).findById("key");

        @SuppressWarnings("unchecked")
        TaskExec<SchedTask> exec = mock(TaskExec.class);
        when(exec.getKey()).thenReturn("execKey");
        when(taskExecDAO.findAll(task, null, null, Pageable.unpaged())).thenReturn(List.of(exec));

        assertEquals(1, logic.deleteExecutions("key", null, null).size());
        verify(taskExecDAO).delete(exec);
    }

    @Test
    void getMacroTaskFormMissing() {
        doReturn(Optional.empty()).when(taskDAO).findById(TaskType.MACRO, "missing");

        assertThrows(NotFoundException.class,
                () -> logic.getMacroTaskForm("missing", java.util.Locale.ENGLISH));
    }

    @Test
    void getJobMissingTask() {
        doReturn(Optional.empty()).when(taskDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.getJob("missing"));
    }

    @Test
    void getJob() {
        SchedTask task = mock(SchedTask.class);
        when(task.getKey()).thenReturn(UUID);
        doReturn(Optional.of(task)).when(taskDAO).findById("key");
        doReturn(Optional.of(task)).when(taskDAO).findById(UUID);
        when(binder.buildRefDesc(task)).thenReturn("desc");
        when(scheduler.getNextTrigger("Master", "taskJob" + UUID)).thenReturn(Optional.empty());
        when(jobManager.isRunning("taskJob" + UUID)).thenReturn(false);
        when(jobStatusDAO.get("taskJob" + UUID)).thenReturn("status");

        JobTO job = logic.getJob("key");

        assertEquals(JobType.TASK, job.getType());
        assertEquals(UUID, job.getRefKey());
        assertEquals("desc", job.getRefDesc());
        assertEquals("status", job.getStatus());
    }

    @Test
    void actionJob() {
        SchedTask task = mock(SchedTask.class);
        when(task.getKey()).thenReturn(UUID);
        doReturn(Optional.of(task)).when(taskDAO).findById("key");

        logic.actionJob("key", JobAction.START);

        verify(scheduler).start("Master", "taskJob" + UUID);
    }

    @Test
    void listJobs() {
        when(scheduler.getJobNames("Master")).thenReturn(List.of());

        assertEquals(List.of(), logic.listJobs());
    }

    @Test
    void purgePropagations() {
        List<PropagationStatus> statuses = List.of();
        when(taskDAO.purgePropagations(null, null, null)).thenReturn(List.of());

        assertEquals(statuses, logic.purgePropagations(null, null, null));
    }

    @Test
    void resolveReference() throws Exception {
        SchedTask task = mock(SchedTask.class);
        TaskUtils taskUtils = mock(TaskUtils.class);
        SchedTaskTO to = new SchedTaskTO();
        to.setKey("key");

        doReturn(Optional.of(task)).when(taskDAO).findById("key");
        when(taskUtilsFactory.getInstance(task)).thenReturn(taskUtils);
        doReturn(to).when(binder).getTaskTO(task, taskUtils, false);

        assertSame(to, logic.resolveReference(
                TaskLogic.class.getMethod("read", TaskType.class, String.class, boolean.class),
                TaskType.SCHEDULED,
                "key",
                false));
    }

    @Test
    void resolveUnresolvedReference() throws Exception {
        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveReference(
                TaskLogic.class.getMethod("listRecentExecutions", int.class), 10));
    }
}
