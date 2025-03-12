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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.WorkflowTask;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.flowable.api.UserRequestHandler;
import org.apache.syncope.core.flowable.api.WorkflowTaskManager;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.provisioning.api.rules.RuleProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.apache.syncope.core.workflow.java.AbstractUserWorkflowAdapter;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;

public class FlowableUserWorkflowAdapter extends AbstractUserWorkflowAdapter implements WorkflowTaskManager {

    protected final DomainProcessEngine engine;

    protected final UserRequestHandler userRequestHandler;

    public FlowableUserWorkflowAdapter(
            final UserDataBinder dataBinder,
            final UserDAO userDAO,
            final RealmDAO realmDAO,
            final GroupDAO groupDAO,
            final EntityFactory entityFactory,
            final SecurityProperties securityProperties,
            final RuleProvider ruleProvider,
            final DomainProcessEngine engine,
            final UserRequestHandler userRequestHandler,
            final ApplicationEventPublisher publisher,
            final EncryptorManager encryptorManager) {

        super(
                dataBinder,
                userDAO,
                realmDAO,
                groupDAO,
                entityFactory,
                securityProperties,
                ruleProvider,
                publisher,
                encryptorManager);
        this.engine = engine;
        this.userRequestHandler = userRequestHandler;
    }

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
            final UserCR userCR,
            final boolean disablePwdPolicyCheck,
            final Boolean enabled,
            final String creator,
            final String context) {

        Map<String, Object> variables = new HashMap<>();
        variables.put(FlowableRuntimeUtils.WF_EXECUTOR, AuthContextUtils.getUsername());
        variables.put(FlowableRuntimeUtils.USER_CR, userCR);
        variables.put(FlowableRuntimeUtils.ENABLED, enabled);

        ProcessInstance procInst = null;
        try {
            procInst = engine.getRuntimeService().
                    startProcessInstanceByKey(FlowableRuntimeUtils.WF_PROCESS_ID, variables);
        } catch (FlowableException e) {
            FlowableRuntimeUtils.throwException(
                    e, "While starting " + FlowableRuntimeUtils.WF_PROCESS_ID + " instance");
        }

        engine.getRuntimeService().removeVariable(
                Objects.requireNonNull(procInst).getProcessInstanceId(), FlowableRuntimeUtils.WF_EXECUTOR);
        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.USER_CR);
        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.USER_TO);

        User user = engine.getRuntimeService().
                getVariable(procInst.getProcessInstanceId(), FlowableRuntimeUtils.USER, User.class);
        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.USER);

        Boolean updatedEnabled = engine.getRuntimeService().
                getVariable(procInst.getProcessInstanceId(), FlowableRuntimeUtils.ENABLED, Boolean.class);
        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.ENABLED);
        Optional.ofNullable(updatedEnabled).ifPresent(ue -> user.setSuspended(!ue));

        metadata(user, creator, context);
        FlowableRuntimeUtils.updateStatus(engine, procInst.getProcessInstanceId(), user);
        User created = userDAO.save(user);

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.CREATE, created, AuthContextUtils.getDomain()));

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
                dataBinder.getUserTO(created, true),
                userCR.getPassword(),
                enabled,
                propByRes,
                propByLinkedAccount);

        Set<String> tasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInst.getProcessInstanceId());

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

        Set<String> preTasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInstID);

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
                engine.getTaskService().complete(tasks.getFirst().getId(), variables);
                task = tasks.getFirst().getTaskDefinitionKey();
            } catch (FlowableException e) {
                FlowableRuntimeUtils.throwException(
                        e, "While completing task '" + tasks.getFirst().getName() + "' for " + user);
            }
        } else {
            LOG.warn("Expected a single task, found {}", tasks.size());
        }

        Set<String> postTasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInstID);
        postTasks.removeAll(preTasks);
        if (task != null) {
            postTasks.add(task);
        }

        return postTasks;
    }

    @Override
    protected UserWorkflowResult<String> doActivate(
            final User user, final String token, final String updater, final String context) {

        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Map<String, Object> variables = new HashMap<>(2);
        variables.put(FlowableRuntimeUtils.TOKEN, token);
        variables.put(FlowableRuntimeUtils.TASK, "activate");

        Set<String> tasks = doExecuteNextTask(procInstID, user, variables);

        metadata(user, updater, context);
        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        User updated = userDAO.save(user);

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

        variables.keySet().forEach(key -> engine.getRuntimeService().removeVariable(procInstID, key));
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);

        return new UserWorkflowResult<>(updated.getKey(), null, null, tasks);
    }

    @Override
    protected UserWorkflowResult<Pair<UserUR, Boolean>> doUpdate(
            final User user, final UserUR userUR, final String updater, final String context) {

        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        // save some existing variable values for later processing, after actual update is made 
        UserUR beforeUpdate = engine.getRuntimeService().
                getVariable(procInstID, FlowableRuntimeUtils.USER_UR, UserUR.class);
        @SuppressWarnings("unchecked")
        PropagationByResource<String> propByResBeforeUpdate = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
        @SuppressWarnings("unchecked")
        PropagationByResource<Pair<String, String>> propByLinkedAccountBeforeUpdate = engine.getRuntimeService().
                getVariable(procInstID, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, PropagationByResource.class);

        // whether the initial status is a form task
        boolean inFormTask = FlowableRuntimeUtils.getFormTask(engine, procInstID) != null;

        Map<String, Object> variables = new HashMap<>(2);
        variables.put(FlowableRuntimeUtils.USER_UR, userUR);
        variables.put(FlowableRuntimeUtils.TASK, "update");

        Set<String> tasks = doExecuteNextTask(procInstID, user, variables);

        metadata(user, updater, context);
        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        User updated = userDAO.save(user);

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.TASK);

        // if the original status was a form task, restore the patch as before the process started
        if (inFormTask) {
            if (beforeUpdate == null) {
                engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER_UR);
            } else {
                engine.getRuntimeService().setVariable(procInstID, FlowableRuntimeUtils.USER_UR, beforeUpdate);
            }
        }

        // whether the after status is a form task
        inFormTask = FlowableRuntimeUtils.getFormTask(engine, procInstID) != null;
        if (!inFormTask) {
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER_UR);
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
                dataBinder.getUserTO(updated, true),
                userUR.getPassword() == null ? null : userUR.getPassword().getValue(),
                null,
                Optional.ofNullable(propByResBeforeUpdate).orElse(propByRes),
                Optional.ofNullable(propByLinkedAccountBeforeUpdate).orElse(propByLinkedAccount));

        if (inFormTask) {
            @SuppressWarnings("unchecked")
            PropagationByResource<String> propByResAfterForm = engine.getRuntimeService().getVariable(
                    procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
            propByRes = propByResAfterForm;
        }

        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE, Boolean.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE);

        return new UserWorkflowResult<>(Pair.of(userUR, propagateEnable), propByRes, propByLinkedAccount, tasks);
    }

    @Override
    protected UserWorkflowResult<String> doSuspend(final User user, final String updater, final String context) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Set<String> performedTasks =
                doExecuteNextTask(procInstID, user, Map.of(FlowableRuntimeUtils.TASK, "suspend"));

        metadata(user, updater, context);
        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        User updated = userDAO.save(user);

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

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
    protected UserWorkflowResult<String> doReactivate(final User user, final String updater, final String context) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Set<String> performedTasks =
                doExecuteNextTask(procInstID, user, Map.of(FlowableRuntimeUtils.TASK, "reactivate"));

        metadata(user, updater, context);
        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        User updated = userDAO.save(user);

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

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
    protected void doRequestPasswordReset(final User user, final String updater, final String context) {
        Map<String, Object> variables = new HashMap<>(3);
        variables.put(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));
        variables.put(FlowableRuntimeUtils.TASK, "requestPasswordReset");
        variables.put(FlowableRuntimeUtils.EVENT, "requestPasswordReset");

        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        doExecuteNextTask(procInstID, user, variables);

        metadata(user, updater, context);
        User updated = userDAO.save(user);

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

        variables.keySet().forEach(key -> engine.getRuntimeService().removeVariable(procInstID, key));
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);
    }

    @Override
    protected UserWorkflowResult<Pair<UserUR, Boolean>> doConfirmPasswordReset(
            final User user, final String token, final String password, final String updater, final String context) {

        Map<String, Object> variables = new HashMap<>(5);
        variables.put(FlowableRuntimeUtils.TOKEN, token);
        variables.put(FlowableRuntimeUtils.PASSWORD, password);
        variables.put(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));
        variables.put(FlowableRuntimeUtils.TASK, "confirmPasswordReset");
        variables.put(FlowableRuntimeUtils.EVENT, "confirmPasswordReset");

        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Set<String> tasks = doExecuteNextTask(procInstID, user, variables);

        metadata(user, updater, context);
        User updated = userDAO.save(user);

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

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
        UserUR updatedReq = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.USER_UR, UserUR.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER_UR);
        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE, Boolean.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE);

        return new UserWorkflowResult<>(Pair.of(updatedReq, propagateEnable), propByRes, propByLinkedAccount, tasks);
    }

    @Override
    protected void doDelete(final User user, final String eraser, final String context) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        doExecuteNextTask(procInstID, user, Map.of(FlowableRuntimeUtils.TASK, "delete"));

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.set(ResourceOperation.DELETE, userDAO.findAllResourceKeys(user.getKey()));

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        user.getLinkedAccounts().forEach(account -> propByLinkedAccount.add(
                ResourceOperation.DELETE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

        if (engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInstID).active().list().isEmpty()) {

            userDAO.deleteById(user.getKey());

            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.DELETE, user, AuthContextUtils.getDomain()));

            if (!engine.getHistoryService().createHistoricProcessInstanceQuery().
                    processInstanceId(procInstID).list().isEmpty()) {

                engine.getHistoryService().deleteHistoricProcessInstance(procInstID);
            }
        } else {
            FlowableRuntimeUtils.saveForFormSubmit(
                    engine,
                    procInstID,
                    dataBinder.getUserTO(user, true),
                    null,
                    null,
                    propByRes,
                    propByLinkedAccount);

            FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
            metadata(user, eraser, context);
            User updated = userDAO.save(user);

            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, updated, AuthContextUtils.getDomain()));

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
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, user, AuthContextUtils.getDomain()));

        engine.getRuntimeService().setVariable(
                procInstID, FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));

        if (engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInstID).active().list().isEmpty()) {

            userDAO.deleteById(user.getKey());

            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.DELETE, user, AuthContextUtils.getDomain()));

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
                    dataBinder.getUserTO(user, true),
                    null,
                    null,
                    propByRes,
                    propByLinkedAccount);
        }

        return new UserWorkflowResult<>(user.getKey(), null, null, performedTasks);
    }

    protected static void navigateAvailableTasks(final FlowElement flow, final List<String> availableTasks) {
        if (flow instanceof Gateway gateway) {
            gateway.getOutgoingFlows().forEach(subflow -> navigateAvailableTasks(subflow, availableTasks));
        } else if (flow instanceof SequenceFlow sequenceFlow) {
            navigateAvailableTasks(sequenceFlow.getTargetFlowElement(), availableTasks);
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
                            engine, FlowableRuntimeUtils.WF_PROCESS_ID).getId()).getProcesses().getFirst();
            process.getFlowElements().stream().
                    filter(SequenceFlow.class::isInstance).
                    map(SequenceFlow.class::cast).
                    filter(flow -> flow.getSourceRef().equals(currentTask.getTaskDefinitionKey())).
                    forEach(flow -> navigateAvailableTasks(flow.getTargetFlowElement(), availableTasks));
        } catch (FlowableException e) {
            throw new WorkflowException(
                    "While reading available tasks for workflow instance " + procInstID, e);
        }

        return availableTasks.stream().map(input -> {
            WorkflowTask workflowTaskTO = new WorkflowTask();
            workflowTaskTO.setName(input);
            return workflowTaskTO;
        }).toList();
    }
}
