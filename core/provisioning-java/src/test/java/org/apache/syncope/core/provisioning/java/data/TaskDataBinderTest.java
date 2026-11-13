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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.ThreadPoolSettings;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TaskDataBinderTest extends AbstractDataBinderTest {

    private static final String SCHED_TASK_KEY = "e95555d2-1b09-42c8-b25b-f4c4ec597979";

    private static final String PULL_TASK_KEY = "c41b9b71-9bfa-4f90-89f2-84787def4c5c";

    private static final String PROPAGATION_TASK_KEY = "1e697572-b896-484c-ae7f-0c8f63fcbc6c";

    @Autowired
    private TaskDataBinder binder;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskUtilsFactory taskUtilsFactory;

    @Test
    void getSchedTaskTO() {
        SchedTask task = taskDAO.<SchedTask>findById(TaskType.SCHEDULED, SCHED_TASK_KEY).orElseThrow();

        SchedTaskTO taskTO = binder.getTaskTO(task, taskUtilsFactory.getInstance(task), true);
        assertEquals(SCHED_TASK_KEY, taskTO.getKey());
        assertEquals("SampleJob Task", taskTO.getName());
        assertTrue(taskTO.isActive());
        assertEquals("0 0 0 1 * ?", taskTO.getCronExpression());
        assertEquals("TestSampleJobDelegate", taskTO.getJobDelegate());
    }

    @Test
    void getPullTaskTO() {
        PullTask task = (PullTask) taskDAO.<SchedTask>findById(TaskType.PULL, PULL_TASK_KEY).orElseThrow();

        PullTaskTO taskTO = binder.getTaskTO(task, taskUtilsFactory.getInstance(task), true);
        assertEquals(PULL_TASK_KEY, taskTO.getKey());
        assertEquals("CSV (update matching; assign unmatching)", taskTO.getName());
        assertEquals("resource-csv", taskTO.getResource());
        assertTrue(taskTO.isPerformCreate());
        assertTrue(taskTO.isPerformUpdate());
        assertTrue(taskTO.isPerformDelete());
        assertEquals(MatchingRule.UPDATE, taskTO.getMatchingRule());
        assertEquals(UnmatchingRule.ASSIGN, taskTO.getUnmatchingRule());
        assertEquals(SyncopeConstants.ROOT_REALM, taskTO.getDestinationRealm());
        assertTrue(taskTO.getTemplates().containsKey(AnyTypeKind.USER.name()));
    }

    @Test
    void getPropagationTaskTO() {
        PropagationTask task =
                taskDAO.<PropagationTask>findById(TaskType.PROPAGATION, PROPAGATION_TASK_KEY).orElseThrow();

        PropagationTaskTO taskTO = binder.getTaskTO(task, taskUtilsFactory.getInstance(task), true);
        assertEquals(PROPAGATION_TASK_KEY, taskTO.getKey());
        assertEquals(ResourceOperation.UPDATE, taskTO.getOperation());
        assertEquals("__ACCOUNT__", taskTO.getObjectClassName());
        assertEquals("ws-target-resource-2", taskTO.getResource());
        assertEquals(AnyTypeKind.USER, taskTO.getAnyTypeKind());
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", taskTO.getEntityKey());
        assertEquals(1, taskTO.getExecutions().size());

        ExecTO execTO = binder.getExecTO(task.getExecs().getFirst());
        assertEquals(task.getKey(), execTO.getRefKey());
        assertEquals(JobType.TASK, execTO.getJobType());
        assertEquals("SUCCESS", execTO.getStatus());
        assertNotNull(execTO.getRefDesc());
        assertTrue(execTO.getRefDesc().contains(PROPAGATION_TASK_KEY));

        PropagationTaskTO withoutDetails = binder.getTaskTO(task, taskUtilsFactory.getInstance(task), false);
        assertTrue(withoutDetails.getExecutions().isEmpty());

        assertTrue(binder.buildRefDesc(task).contains(PROPAGATION_TASK_KEY));
    }

    @Test
    void createSchedTask() {
        SchedTaskTO taskTO = new SchedTaskTO();
        taskTO.setName("New sched task");
        taskTO.setDescription("description");
        taskTO.setCronExpression("0 0 1 * * ?");
        taskTO.setActive(true);
        taskTO.setJobDelegate("TestSampleJobDelegate");

        TaskUtils taskUtils = taskUtilsFactory.getInstance(TaskType.SCHEDULED);
        SchedTask task = binder.createSchedTask(taskTO, taskUtils);
        task = taskDAO.save(task);
        entityManager.flush();

        assertNotNull(task.getKey());

        SchedTaskTO actual = binder.getTaskTO(task, taskUtils, true);
        assertEquals("New sched task", actual.getName());
        assertEquals("description", actual.getDescription());
        assertEquals("0 0 1 * * ?", actual.getCronExpression());
        assertTrue(actual.isActive());
        assertEquals("TestSampleJobDelegate", actual.getJobDelegate());
    }

    @Test
    void createPullTask() {
        ThreadPoolSettings concurrentSettings = new ThreadPoolSettings();
        concurrentSettings.setPoolSize(5);

        PullTaskTO taskTO = new PullTaskTO();
        taskTO.setName("New pull task");
        taskTO.setDescription("pull description");
        taskTO.setCronExpression("0 0 2 * * ?");
        taskTO.setActive(true);
        taskTO.setResource("resource-csv");
        taskTO.setDestinationRealm("/even/two");
        taskTO.setPullMode(PullMode.FULL_RECONCILIATION);
        taskTO.setReconFilterBuilder("not-existing");
        taskTO.setMatchingRule(MatchingRule.UPDATE);
        taskTO.setUnmatchingRule(UnmatchingRule.ASSIGN);
        taskTO.setPerformCreate(true);
        taskTO.setPerformUpdate(true);
        taskTO.setPerformDelete(false);
        taskTO.setSyncStatus(true);
        taskTO.setRemediation(true);
        taskTO.setConcurrentSettings(concurrentSettings);
        taskTO.getActions().add("LDAPMembershipPullActions");
        taskTO.getActions().add("not-existing");

        UserTO template = new UserTO();
        template.setUsername("'templatedUser'");
        template.getPlainAttrs().add(new Attr.Builder("firstname").value("'Mario'").build());
        template.getPlainAttrs().add(new Attr.Builder("surname").value("'Rossi'").build());
        taskTO.getTemplates().put(AnyTypeKind.USER.name(), template);

        TaskUtils taskUtils = taskUtilsFactory.getInstance(TaskType.PULL);
        PullTask task = (PullTask) binder.createSchedTask(taskTO, taskUtils);
        task = taskDAO.save(task);
        entityManager.flush();

        PullTaskTO actual = binder.getTaskTO(task, taskUtils, true);
        assertEquals("New pull task", actual.getName());
        assertEquals("pull description", actual.getDescription());
        assertEquals("0 0 2 * * ?", actual.getCronExpression());
        assertEquals("resource-csv", actual.getResource());
        assertEquals("/even/two", actual.getDestinationRealm());
        assertEquals(PullMode.FULL_RECONCILIATION, actual.getPullMode());
        assertNull(actual.getReconFilterBuilder());
        assertEquals(MatchingRule.UPDATE, actual.getMatchingRule());
        assertEquals(UnmatchingRule.ASSIGN, actual.getUnmatchingRule());
        assertTrue(actual.isPerformCreate());
        assertTrue(actual.isPerformUpdate());
        assertFalse(actual.isPerformDelete());
        assertTrue(actual.isSyncStatus());
        assertTrue(actual.isRemediation());
        assertEquals(List.of("LDAPMembershipPullActions"), actual.getActions());
        assertEquals(5, actual.getConcurrentSettings().getPoolSize());
        assertTrue(actual.getTemplates().containsKey(AnyTypeKind.USER.name()));
    }

    @Test
    void createPushTask() {
        PushTaskTO taskTO = new PushTaskTO();
        taskTO.setName("New push task");
        taskTO.setActive(true);
        taskTO.setResource("ws-target-resource-1");
        taskTO.setSourceRealm("/even");
        taskTO.setMatchingRule(MatchingRule.LINK);
        taskTO.setUnmatchingRule(UnmatchingRule.IGNORE);
        taskTO.setPerformCreate(true);
        taskTO.setPerformUpdate(true);
        taskTO.setPerformDelete(true);
        taskTO.getFilters().put(AnyTypeKind.USER.name(), "username==\"rossini\"");
        taskTO.getFilters().put("not-existing", "username==\"rossini\"");

        TaskUtils taskUtils = taskUtilsFactory.getInstance(TaskType.PUSH);
        SchedTask task = binder.createSchedTask(taskTO, taskUtils);
        task = taskDAO.save(task);
        entityManager.flush();

        PushTaskTO actual = binder.getTaskTO(task, taskUtils, true);
        assertEquals("New push task", actual.getName());
        assertEquals("ws-target-resource-1", actual.getResource());
        assertEquals("/even", actual.getSourceRealm());
        assertEquals(MatchingRule.LINK, actual.getMatchingRule());
        assertEquals(UnmatchingRule.IGNORE, actual.getUnmatchingRule());
        assertTrue(actual.getFilters().containsKey(AnyTypeKind.USER.name()));
        assertFalse(actual.getFilters().containsKey("not-existing"));
    }

    @Test
    void updateSchedTask() {
        SchedTask task = taskDAO.<SchedTask>findById(TaskType.SCHEDULED, SCHED_TASK_KEY).orElseThrow();
        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        SchedTaskTO taskTO = binder.getTaskTO(task, taskUtils, true);
        taskTO.setName("Updated task");
        taskTO.setDescription("updated description");
        taskTO.setCronExpression("0 0 3 * * ?");
        taskTO.setActive(false);

        binder.updateSchedTask(task, taskTO, taskUtils);
        entityManager.flush();

        SchedTaskTO actual = binder.getTaskTO(task, taskUtils, true);
        assertEquals("Updated task", actual.getName());
        assertEquals("updated description", actual.getDescription());
        assertEquals("0 0 3 * * ?", actual.getCronExpression());
        assertFalse(actual.isActive());

        SchedTaskTO blankName = binder.getTaskTO(task, taskUtils, false);
        blankName.setName("   ");
        assertThrows(SyncopeClientException.class, () -> binder.updateSchedTask(task, blankName, taskUtils));

        assertThrows(IllegalArgumentException.class,
                () -> binder.updateSchedTask(task, new PullTaskTO(), taskUtils));
        assertThrows(IllegalArgumentException.class,
                () -> binder.createSchedTask(new SchedTaskTO(), taskUtilsFactory.getInstance(TaskType.PULL)));
    }

    @Test
    void updatePullTask() {
        PullTask task = (PullTask) taskDAO.<SchedTask>findById(TaskType.PULL, PULL_TASK_KEY).orElseThrow();
        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        PullTaskTO taskTO = binder.getTaskTO(task, taskUtils, true);
        taskTO.setName("Updated pull task");
        taskTO.setDestinationRealm("/even/two");
        taskTO.setMatchingRule(MatchingRule.LINK);
        taskTO.setUnmatchingRule(UnmatchingRule.IGNORE);
        taskTO.setPerformCreate(false);
        taskTO.setPullMode(PullMode.INCREMENTAL);
        taskTO.getActions().clear();
        taskTO.getActions().add("LDAPMembershipPullActions");
        taskTO.getTemplates().clear();

        binder.updateSchedTask(task, taskTO, taskUtils);
        entityManager.flush();

        PullTaskTO actual = binder.getTaskTO(task, taskUtils, true);
        assertEquals("Updated pull task", actual.getName());
        assertEquals("/even/two", actual.getDestinationRealm());
        assertEquals(MatchingRule.LINK, actual.getMatchingRule());
        assertEquals(UnmatchingRule.IGNORE, actual.getUnmatchingRule());
        assertFalse(actual.isPerformCreate());
        assertEquals(PullMode.INCREMENTAL, actual.getPullMode());
        assertEquals(List.of("LDAPMembershipPullActions"), actual.getActions());
        assertTrue(actual.getTemplates().isEmpty());
    }

    @Test
    void invalidPullTaskTemplate() {
        PullTaskTO taskTO = new PullTaskTO();
        taskTO.setName("Invalid pull task");
        taskTO.setResource("resource-csv");
        taskTO.setDestinationRealm("/even");
        taskTO.setPullMode(PullMode.INCREMENTAL);

        UserTO template = new UserTO();
        template.setUsername("not a valid ((");
        taskTO.getTemplates().put(AnyTypeKind.USER.name(), template);

        assertThrows(SyncopeClientException.class, () -> binder.createSchedTask(
                taskTO, taskUtilsFactory.getInstance(TaskType.PULL)));
    }
}
