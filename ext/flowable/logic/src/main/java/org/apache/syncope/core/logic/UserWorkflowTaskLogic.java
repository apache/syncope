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

import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTask;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.flowable.api.WorkflowTaskManager;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.security.access.prepost.PreAuthorize;

public class UserWorkflowTaskLogic extends AbstractTransactionalLogic<EntityTO> {

    protected final WorkflowTaskManager wfTaskManager;

    protected final PropagationManager propagationManager;

    protected final PropagationTaskExecutor taskExecutor;

    protected final UserDataBinder binder;

    protected final UserDAO userDAO;

    public UserWorkflowTaskLogic(
            final WorkflowTaskManager wfTaskManager,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final UserDataBinder binder,
            final UserDAO userDAO) {

        this.wfTaskManager = wfTaskManager;
        this.propagationManager = propagationManager;
        this.taskExecutor = taskExecutor;
        this.binder = binder;
        this.userDAO = userDAO;
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.WORKFLOW_TASK_LIST + "') "
            + "and hasRole('" + IdRepoEntitlement.USER_READ + "')")
    public List<WorkflowTask> getAvailableTasks(final String key) {
        User user = userDAO.authFind(key);
        return wfTaskManager.getAvailableTasks(user.getKey());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    public UserTO executeNextTask(final WorkflowTaskExecInput workflowTaskExecInput) {
        UserWorkflowResult<String> updated = wfTaskManager.executeNextTask(workflowTaskExecInput);

        UserUR userUR = new UserUR.Builder(updated.getResult()).build();

        List<PropagationTaskInfo> taskInfos = propagationManager.getUserUpdateTasks(
                new UserWorkflowResult<>(
                        Pair.of(userUR, null),
                        updated.getPropByRes(),
                        updated.getPropByLinkedAccount(),
                        updated.getPerformedTasks()));
        taskExecutor.execute(taskInfos, false, AuthContextUtils.getUsername());

        return binder.getUserTO(updated.getResult());
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
