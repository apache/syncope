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
package org.apache.syncope.core.rest.controller;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserWorkflowController extends AbstractController {

    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected NotificationManager notificationManager;

    @Autowired
    protected UserDataBinder binder;

    @PreAuthorize("hasRole('WORKFLOW_FORM_CLAIM')")
    @Transactional(rollbackFor = { Throwable.class })
    public WorkflowFormTO claimForm(final String taskId) {
        WorkflowFormTO result = uwfAdapter.claimForm(taskId,
                SecurityContextHolder.getContext().getAuthentication().getName());

        auditManager.audit(AuditElements.Category.user, AuditElements.UserSubCategory.claimForm,
                AuditElements.Result.success,
                "Successfully claimed workflow form: " + taskId);

        return result;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    public UserTO executeWorkflow(final UserTO userTO, final String taskId) {
        LOG.debug("About to execute {} on {}", taskId, userTO.getId());

        WorkflowResult<Long> updated = uwfAdapter.execute(userTO, taskId);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                        new AbstractMap.SimpleEntry<UserMod, Boolean>(userMod, null),
                        updated.getPropByRes(), updated.getPerformedTasks()));

        taskExecutor.execute(tasks);

        notificationManager.createTasks(updated.getResult(), updated.getPerformedTasks());

        final UserTO savedTO = binder.getUserTO(updated.getResult());

        LOG.debug("About to return updated user\n{}", savedTO);

        auditManager.audit(AuditElements.Category.user, AuditElements.UserSubCategory.executeWorkflow,
                AuditElements.Result.success,
                "Successfully executed workflow action " + taskId + " on user: " + userTO.getUsername());

        return savedTO;
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_READ') and hasRole('USER_READ')")
    @Transactional(rollbackFor = { Throwable.class })
    public WorkflowFormTO getFormForUser(final Long userId) {
        SyncopeUser user = binder.getUserFromId(userId);
        WorkflowFormTO result = uwfAdapter.getForm(user.getWorkflowId());

        auditManager.audit(AuditElements.Category.user, AuditElements.UserSubCategory.getFormForUser,
                AuditElements.Result.success,
                "Successfully read workflow form for user: " + user.getUsername());

        return result;
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_LIST')")
    @Transactional(rollbackFor = { Throwable.class })
    public List<WorkflowFormTO> getForms() {
        List<WorkflowFormTO> forms = uwfAdapter.getForms();

        auditManager.audit(AuditElements.Category.user, AuditElements.UserSubCategory.getForms,
                AuditElements.Result.success,
                "Successfully list workflow forms: " + forms.size());

        return forms;
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_READ') and hasRole('USER_READ')")
    @Transactional(rollbackFor = { Throwable.class })
    public List<WorkflowFormTO> getForms(final Long userId, final String formName) {
        SyncopeUser user = binder.getUserFromId(userId);
        final List<WorkflowFormTO> result = uwfAdapter.getForms(user.getWorkflowId(), formName);

        auditManager.audit(AuditElements.Category.user, AuditElements.UserSubCategory.getFormForUser,
                AuditElements.Result.success,
                "Successfully read workflow form for user: " + user.getUsername());

        return result;
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_SUBMIT')")
    @Transactional(rollbackFor = { Throwable.class })
    public UserTO submitForm(final WorkflowFormTO form) {
        LOG.debug("About to process form {}", form);

        WorkflowResult<? extends AbstractAttributableMod> updated =
                uwfAdapter.submitForm(form, SecurityContextHolder.getContext().getAuthentication().getName());

        // propByRes can be made empty by the workflow definition if no propagation should occur 
        // (for example, with rejected users)
        if (updated.getResult() instanceof UserMod
                && updated.getPropByRes() != null && !updated.getPropByRes().isEmpty()) {

            List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                    new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                            new AbstractMap.SimpleEntry<UserMod, Boolean>((UserMod) updated.getResult(), Boolean.TRUE),
                            updated.getPropByRes(),
                            updated.getPerformedTasks()));

            taskExecutor.execute(tasks);
        }

        UserTO savedTO = binder.getUserTO(updated.getResult().getId());

        auditManager.audit(AuditElements.Category.user, AuditElements.UserSubCategory.submitForm,
                AuditElements.Result.success,
                "Successfully submitted workflow form for : " + savedTO.getUsername());

        LOG.debug("About to return user after form processing\n{}", savedTO);

        return savedTO;
    }
}
