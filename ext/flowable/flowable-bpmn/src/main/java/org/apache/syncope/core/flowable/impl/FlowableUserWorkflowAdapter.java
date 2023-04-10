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
package org.apache.syncope.core.flowable.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTask;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.flowable.api.UserRequestHandler;
import org.apache.syncope.core.flowable.api.WorkflowTaskManager;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.event.AnyLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.apache.syncope.core.workflow.java.AbstractUserWorkflowAdapter;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

public class FlowableUserWorkflowAdapter extends AbstractUserWorkflowAdapter implements WorkflowTaskManager {

    @Autowired
    protected DomainProcessEngine engine;

    @Autowired
    protected UserRequestHandler userRequestHandler;

    @Autowired
    protected ApplicationEventPublisher publisher;

    @Override
    public String getPrefix() {
        return "ACT_";
    }

    @Override
    public <T> T getVariable(final String executionId, final String variableName, final Class<T> variableClass) {
        return engine.getRuntimeService().getVariable(executionId, variableName, variableClass);
    }

    @Override
    public void setVariable(final String executionId, final String variableName, final Object value) {
        engine.getRuntimeService().setVariable(executionId, variableName, value);
    }

    protected User lazyLoad(final User user) {
        // using BeanUtils to access all user's properties and trigger lazy loading - we are about to
        // serialize a User instance for availability within workflow tasks, and this breaks transactions
        BeanUtils.copyProperties(user, entityFactory.newEntity(User.class));
        return user;
    }

    @Override
    protected UserWorkflowResult<Pair<String, Boolean>> doCreate(
            final UserTO userTO,
            final boolean disablePwdPolicyCheck,
            final Boolean enabled,
            final boolean storePassword) {

        Map<String, Object> variables = new HashMap<>();
        variables.put(FlowableRuntimeUtils.WF_EXECUTOR, AuthContextUtils.getUsername());
        variables.put(FlowableRuntimeUtils.USER_TO, userTO);
        variables.put(FlowableRuntimeUtils.ENABLED, enabled);
        variables.put(FlowableRuntimeUtils.STORE_PASSWORD, storePassword);

        ProcessInstance procInst = null;
        try {
            procInst = engine.getRuntimeService().
                    startProcessInstanceByKey(FlowableRuntimeUtils.WF_PROCESS_ID, variables);
        } catch (FlowableException e) {
            FlowableRuntimeUtils.throwException(
                    e, "While starting " + FlowableRuntimeUtils.WF_PROCESS_ID + " instance");
        }

        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.WF_EXECUTOR);
        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.USER_TO);
        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.STORE_PASSWORD);

        User user = engine.getRuntimeService().
                getVariable(procInst.getProcessInstanceId(), FlowableRuntimeUtils.USER, User.class);
        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.USER);

        Boolean updatedEnabled = engine.getRuntimeService().
                getVariable(procInst.getProcessInstanceId(), FlowableRuntimeUtils.ENABLED, Boolean.class);
        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.ENABLED);
        if (updatedEnabled != null) {
            user.setSuspended(!updatedEnabled);
        }

        FlowableRuntimeUtils.updateStatus(engine, procInst.getProcessInstanceId(), user);
        User created = userDAO.save(user);

        publisher.publishEvent(
                new AnyLifecycleEvent<>(this, SyncDeltaType.CREATE, created, AuthContextUtils.getDomain()));

        engine.getRuntimeService().updateBusinessKey(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.getWFProcBusinessKey(created.getKey()));

        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.PROPAGATE_ENABLE, Boolean.class);
        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.PROPAGATE_ENABLE);
        if (propagateEnable == null) {
            propagateEnable = enabled;
        }

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.set(ResourceOperation.CREATE, userDAO.findAllResourceKeys(created.getKey()));

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        user.getLinkedAccounts().forEach(account -> propByLinkedAccount.add(
                ResourceOperation.CREATE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

        FlowableRuntimeUtils.saveForFormSubmit(
                engine,
                procInst.getProcessInstanceId(),
                created,
                dataBinder.getUserTO(created, true),
                userTO.getPassword(),
                enabled,
                propByRes,
                propByLinkedAccount);

        Set<String> tasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInst.getProcessInstanceId(), user);

        return new UserWorkflowResult<>(
                Pair.of(created.getKey(), propagateEnable),
                propByRes,
                propByLinkedAccount,
                tasks);
    }

    protected Set<String> doExecuteNextTask(
            final String procInstID,
            final User user,
            final Map<String, Object> moreVariables) {

        Set<String> preTasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInstID, user);

        Map<String, Object> variables = new HashMap<>();
        variables.put(FlowableRuntimeUtils.WF_EXECUTOR, AuthContextUtils.getUsername());
        variables.put(FlowableRuntimeUtils.USER, lazyLoad(user));

        if (moreVariables != null && !moreVariables.isEmpty()) {
            variables.putAll(moreVariables);
        }

        List<Task> tasks = engine.getTaskService().createTaskQuery().processInstanceId(procInstID).list();
        String task = null;
        if (tasks.size() == 1) {
            try {
                engine.getTaskService().complete(tasks.get(0).getId(), variables);
                task = tasks.get(0).getTaskDefinitionKey();
            } catch (FlowableException e) {
                FlowableRuntimeUtils.throwException(
                        e, "While completing task '" + tasks.get(0).getName() + "' for " + user);
            }
        } else {
            LOG.warn("Expected a single task, found {}", tasks.size());
        }

        Set<String> postTasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInstID, user);
        postTasks.removeAll(preTasks);
        if (task != null) {
            postTasks.add(task);
        }

        return postTasks;
    }

    @Override
    protected UserWorkflowResult<String> doActivate(final User user, final String token) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Map<String, Object> variables = new HashMap<>(2);
        variables.put(FlowableRuntimeUtils.TOKEN, token);
        variables.put(FlowableRuntimeUtils.TASK, "activate");

        Set<String> tasks = doExecuteNextTask(procInstID, user, variables);

        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        User updated = userDAO.save(user);

        publisher.publishEvent(
                new AnyLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

        variables.keySet().forEach(key -> engine.getRuntimeService().removeVariable(procInstID, key));
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);

        return new UserWorkflowResult<>(updated.getKey(), null, null, tasks);
    }

    @Override
    protected UserWorkflowResult<Pair<UserPatch, Boolean>> doUpdate(final User user, final UserPatch userPatch) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        // save some existing variable values for later processing, after actual update is made 
        UserPatch patchBeforeUpdate = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.USER_PATCH, UserPatch.class);
        @SuppressWarnings("unchecked")
        PropagationByResource<String> propByResBeforeUpdate = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
        @SuppressWarnings("unchecked")
        PropagationByResource<Pair<String, String>> propByLinkedAccountBeforeUpdate = engine.getRuntimeService().
                getVariable(procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, PropagationByResource.class);

        // whether the initial status is a form task
        boolean inFormTask = FlowableRuntimeUtils.getFormTask(engine, procInstID) != null;

        Map<String, Object> variables = new HashMap<>(2);
        variables.put(FlowableRuntimeUtils.USER_PATCH, userPatch);
        variables.put(FlowableRuntimeUtils.TASK, "update");

        Set<String> tasks = doExecuteNextTask(procInstID, user, variables);

        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        User updated = userDAO.save(user);

        publisher.publishEvent(
                new AnyLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.TASK);

        // if the original status was a form task, restore the patch as before the process started
        if (inFormTask) {
            if (patchBeforeUpdate == null) {
                engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER_PATCH);
            } else {
                engine.getRuntimeService().setVariable(procInstID, FlowableRuntimeUtils.USER_PATCH, patchBeforeUpdate);
            }
        }

        // whether the after status is a form task
        inFormTask = FlowableRuntimeUtils.getFormTask(engine, procInstID) != null;
        if (!inFormTask) {
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER_PATCH);
        }

        @SuppressWarnings("unchecked")
        PropagationByResource<String> propByRes = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE);

        @SuppressWarnings("unchecked")
        PropagationByResource<Pair<String, String>> propByLinkedAccount = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, PropagationByResource.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT);

        FlowableRuntimeUtils.saveForFormSubmit(
                engine,
                procInstID,
                updated,
                dataBinder.getUserTO(updated, true),
                userPatch.getPassword() == null ? null : userPatch.getPassword().getValue(),
                null,
                propByResBeforeUpdate == null ? propByRes : propByResBeforeUpdate,
                propByLinkedAccountBeforeUpdate == null ? propByLinkedAccount : propByLinkedAccountBeforeUpdate);

        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE, Boolean.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE);

        return new UserWorkflowResult<>(Pair.of(userPatch, propagateEnable), propByRes, propByLinkedAccount, tasks);
    }

    @Override
    protected UserWorkflowResult<String> doSuspend(final User user) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Set<String> performedTasks =
                doExecuteNextTask(procInstID, user, Collections.singletonMap(FlowableRuntimeUtils.TASK, "suspend"));
        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        User updated = userDAO.save(user);

        publisher.publishEvent(
                new AnyLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

        @SuppressWarnings("unchecked")
        PropagationByResource<String> propByRes = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE);

        @SuppressWarnings("unchecked")
        PropagationByResource<Pair<String, String>> propByLinkedAccount = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, PropagationByResource.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT);

        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.TASK);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT);

        return new UserWorkflowResult<>(updated.getKey(), propByRes, propByLinkedAccount, performedTasks);
    }

    @Override
    protected UserWorkflowResult<String> doReactivate(final User user) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Set<String> performedTasks =
                doExecuteNextTask(procInstID, user, Collections.singletonMap(FlowableRuntimeUtils.TASK, "reactivate"));
        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);

        User updated = userDAO.save(user);

        publisher.publishEvent(
                new AnyLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

        @SuppressWarnings("unchecked")
        PropagationByResource<String> propByRes = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE);

        @SuppressWarnings("unchecked")
        PropagationByResource<Pair<String, String>> propByLinkedAccount = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, PropagationByResource.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT);

        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.TASK);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT);

        return new UserWorkflowResult<>(updated.getKey(), propByRes, propByLinkedAccount, performedTasks);
    }

    @Override
    protected void doRequestPasswordReset(final User user) {
        Map<String, Object> variables = new HashMap<>(3);
        variables.put(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));
        variables.put(FlowableRuntimeUtils.TASK, "requestPasswordReset");
        variables.put(FlowableRuntimeUtils.EVENT, "requestPasswordReset");

        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        doExecuteNextTask(procInstID, user, variables);
        User updated = userDAO.save(user);

        publisher.publishEvent(
                new AnyLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

        variables.keySet().forEach(key -> engine.getRuntimeService().removeVariable(procInstID, key));
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);
    }

    @Override
    protected UserWorkflowResult<Pair<UserPatch, Boolean>> doConfirmPasswordReset(
            final User user, final String token, final String password) {

        Map<String, Object> variables = new HashMap<>(5);
        variables.put(FlowableRuntimeUtils.TOKEN, token);
        variables.put(FlowableRuntimeUtils.PASSWORD, password);
        variables.put(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));
        variables.put(FlowableRuntimeUtils.TASK, "confirmPasswordReset");
        variables.put(FlowableRuntimeUtils.EVENT, "confirmPasswordReset");

        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Set<String> tasks = doExecuteNextTask(procInstID, user, variables);

        User updated = userDAO.save(user);

        publisher.publishEvent(
                new AnyLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

        variables.keySet().forEach(key -> engine.getRuntimeService().removeVariable(procInstID, key));
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);

        @SuppressWarnings("unchecked")
        PropagationByResource<String> propByRes = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE);
        @SuppressWarnings("unchecked")
        PropagationByResource<Pair<String, String>> propByLinkedAccount = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, PropagationByResource.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT);
        UserPatch updatedPatch = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.USER_PATCH, UserPatch.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER_PATCH);
        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE, Boolean.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE);

        return new UserWorkflowResult<>(Pair.of(updatedPatch, propagateEnable), propByRes, propByLinkedAccount, tasks);
    }

    @Override
    protected void doDelete(final User user) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        doExecuteNextTask(procInstID, user, Collections.singletonMap(FlowableRuntimeUtils.TASK, "delete"));

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.set(ResourceOperation.DELETE, userDAO.findAllResourceKeys(user.getKey()));

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        user.getLinkedAccounts().forEach(account -> propByLinkedAccount.add(
                ResourceOperation.DELETE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

        if (engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInstID).active().list().isEmpty()) {

            userDAO.delete(user.getKey());

            publisher.publishEvent(
                    new AnyLifecycleEvent<>(this, SyncDeltaType.DELETE, user, AuthContextUtils.getDomain()));

            if (!engine.getHistoryService().createHistoricProcessInstanceQuery().
                    processInstanceId(procInstID).list().isEmpty()) {

                engine.getHistoryService().deleteHistoricProcessInstance(procInstID);
            }
        } else {
            FlowableRuntimeUtils.saveForFormSubmit(
                    engine,
                    procInstID,
                    user,
                    dataBinder.getUserTO(user, true),
                    null,
                    null,
                    propByRes,
                    propByLinkedAccount);

            FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
            User updated = userDAO.save(user);

            publisher.publishEvent(
                    new AnyLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.TASK);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);
        }
    }

    @Override
    public UserWorkflowResult<String> executeNextTask(final WorkflowTaskExecInput workflowTaskExecInput) {
        User user = userDAO.authFind(workflowTaskExecInput.getUserKey());

        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Map<String, Object> variables = new HashMap<>();
        variables.put(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));
        variables.putAll(workflowTaskExecInput.getVariables());

        Set<String> performedTasks = doExecuteNextTask(procInstID, user, variables);
        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        user = userDAO.save(user);

        publisher.publishEvent(
                new AnyLifecycleEvent<>(this, SyncDeltaType.UPDATE, user, AuthContextUtils.getDomain()));

        engine.getRuntimeService().setVariable(
                procInstID, FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));

        if (engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInstID).active().list().isEmpty()) {

            userDAO.delete(user.getKey());

            publisher.publishEvent(
                    new AnyLifecycleEvent<>(this, SyncDeltaType.DELETE, user, AuthContextUtils.getDomain()));

            if (!engine.getHistoryService().createHistoricProcessInstanceQuery().
                    processInstanceId(procInstID).list().isEmpty()) {

                engine.getHistoryService().deleteHistoricProcessInstance(procInstID);
            }
        } else {
            @SuppressWarnings("unchecked")
            PropagationByResource<String> propByRes = engine.getRuntimeService().
                    getVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
            @SuppressWarnings("unchecked")
            PropagationByResource<Pair<String, String>> propByLinkedAccount = engine.getRuntimeService().getVariable(
                    procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, PropagationByResource.class);

            FlowableRuntimeUtils.saveForFormSubmit(
                    engine,
                    procInstID,
                    user,
                    dataBinder.getUserTO(user, true),
                    null,
                    null,
                    propByRes,
                    propByLinkedAccount);
        }

        return new UserWorkflowResult<>(user.getKey(), null, null, performedTasks);
    }

    protected void navigateAvailableTasks(final FlowElement flow, final List<String> availableTasks) {
        if (flow instanceof Gateway) {
            ((Gateway) flow).getOutgoingFlows().forEach(subflow -> navigateAvailableTasks(subflow, availableTasks));
        } else if (flow instanceof SequenceFlow) {
            navigateAvailableTasks(((SequenceFlow) flow).getTargetFlowElement(), availableTasks);
        } else if (flow instanceof org.flowable.bpmn.model.Task) {
            availableTasks.add(flow.getId());
        } else {
            LOG.debug("Unexpected flow found: {}", flow);
        }
    }

    @Override
    public List<WorkflowTask> getAvailableTasks(final String userKey) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, userKey);

        List<String> availableTasks = new ArrayList<>();
        try {
            Task currentTask = engine.getTaskService().createTaskQuery().processInstanceId(procInstID).singleResult();

            Process process = engine.getRepositoryService().
                    getBpmnModel(FlowableRuntimeUtils.getLatestProcDefByKey(
                            engine, FlowableRuntimeUtils.WF_PROCESS_ID).getId()).getProcesses().get(0);
            process.getFlowElements().stream().
                    filter(SequenceFlow.class::isInstance).
                    map(SequenceFlow.class::cast).
                    filter(sequenceFlow -> sequenceFlow.getSourceRef().equals(currentTask.getTaskDefinitionKey())).
                    forEach(sequenceFlow -> {
                        navigateAvailableTasks(sequenceFlow.getTargetFlowElement(), availableTasks);
                    });
        } catch (FlowableException e) {
            throw new WorkflowException(
                    "While reading available tasks for workflow instance " + procInstID, e);
        }

        return availableTasks.stream().map(input -> {
            WorkflowTask workflowTaskTO = new WorkflowTask();
            workflowTaskTO.setName(input);
            return workflowTaskTO;
        }).collect(Collectors.toList());
    }
}
