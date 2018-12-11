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
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.WorkflowTask;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.flowable.api.UserRequestHandler;
import org.apache.syncope.core.flowable.api.WorkflowTaskManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.apache.syncope.core.workflow.java.AbstractUserWorkflowAdapter;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class FlowableUserWorkflowAdapter extends AbstractUserWorkflowAdapter implements WorkflowTaskManager {

    @Autowired
    protected DomainProcessEngine engine;

    @Autowired
    protected UserRequestHandler userRequestHandler;

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
    protected WorkflowResult<Pair<String, Boolean>> doCreate(
            final UserCR userCR,
            final boolean disablePwdPolicyCheck,
            final Boolean enabled) {

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
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.WF_EXECUTOR);
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
        if (updatedEnabled != null) {
            user.setSuspended(!updatedEnabled);
        }

        // this will make UserValidator not to consider password policies at all
        if (disablePwdPolicyCheck) {
            user.removeClearPassword();
        }

        FlowableRuntimeUtils.updateStatus(engine, procInst.getProcessInstanceId(), user);
        User created = userDAO.save(user);

        engine.getRuntimeService().updateBusinessKey(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.getWFProcBusinessKey(created.getKey()));

        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.PROPAGATE_ENABLE, Boolean.class);
        engine.getRuntimeService().removeVariable(
                procInst.getProcessInstanceId(), FlowableRuntimeUtils.PROPAGATE_ENABLE);
        if (propagateEnable == null) {
            propagateEnable = enabled;
        }

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.CREATE, userDAO.findAllResourceKeys(created.getKey()));

        FlowableRuntimeUtils.saveForFormSubmit(
                engine,
                procInst.getProcessInstanceId(),
                created,
                dataBinder.getUserTO(created, true),
                userCR.getPassword(),
                enabled,
                propByRes);

        Set<String> tasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInst.getProcessInstanceId(), user);

        return new WorkflowResult<>(Pair.of(created.getKey(), propagateEnable), propByRes, tasks);
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

        List<Task> tasks = FlowableRuntimeUtils.createTaskQuery(engine, false).processInstanceId(procInstID).list();
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
    protected WorkflowResult<String> doActivate(final User user, final String token) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Map<String, Object> variables = new HashMap<>(2);
        variables.put(FlowableRuntimeUtils.TOKEN, token);
        variables.put(FlowableRuntimeUtils.TASK, "activate");

        Set<String> tasks = doExecuteNextTask(procInstID, user, variables);

        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        User updated = userDAO.save(user);

        variables.keySet().forEach(key -> engine.getRuntimeService().removeVariable(procInstID, key));
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);

        return new WorkflowResult<>(updated.getKey(), null, tasks);
    }

    @Override
    protected WorkflowResult<Pair<UserUR, Boolean>> doUpdate(final User user, final UserUR userUR) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        // save some existing variable values for later processing, after actual update is made 
        UserUR beforeUpdate = engine.getRuntimeService().
                getVariable(procInstID, FlowableRuntimeUtils.USER_UR, UserUR.class);
        PropagationByResource propByResBeforeUpdate = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);

        // whether the initial status is a form task
        boolean inFormTask = FlowableRuntimeUtils.getFormTask(engine, procInstID) != null;

        Map<String, Object> variables = new HashMap<>(2);
        variables.put(FlowableRuntimeUtils.USER_UR, userUR);
        variables.put(FlowableRuntimeUtils.TASK, "update");

        Set<String> tasks = doExecuteNextTask(procInstID, user, variables);

        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        User updated = userDAO.save(user);

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

        PropagationByResource propByRes = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE);

        FlowableRuntimeUtils.saveForFormSubmit(
                engine,
                procInstID,
                updated,
                dataBinder.getUserTO(updated, true),
                userUR.getPassword() == null ? null : userUR.getPassword().getValue(),
                null,
                propByResBeforeUpdate == null ? propByRes : propByResBeforeUpdate);

        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE, Boolean.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE);

        return new WorkflowResult<>(Pair.of(userUR, propagateEnable), propByRes, tasks);
    }

    @Override
    protected WorkflowResult<String> doSuspend(final User user) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Set<String> performedTasks =
                doExecuteNextTask(procInstID, user, Collections.singletonMap(FlowableRuntimeUtils.TASK, "suspend"));
        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        User updated = userDAO.save(user);

        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.TASK);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);

        return new WorkflowResult<>(updated.getKey(), null, performedTasks);
    }

    @Override
    protected WorkflowResult<String> doReactivate(final User user) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Set<String> performedTasks =
                doExecuteNextTask(procInstID, user, Collections.singletonMap(FlowableRuntimeUtils.TASK, "reactivate"));
        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);

        User updated = userDAO.save(user);

        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.TASK);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);

        return new WorkflowResult<>(updated.getKey(), null, performedTasks);
    }

    @Override
    protected void doRequestPasswordReset(final User user) {
        Map<String, Object> variables = new HashMap<>(3);
        variables.put(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));
        variables.put(FlowableRuntimeUtils.TASK, "requestPasswordReset");
        variables.put(FlowableRuntimeUtils.EVENT, "requestPasswordReset");

        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        doExecuteNextTask(procInstID, user, variables);
        userDAO.save(user);

        variables.keySet().forEach(key -> engine.getRuntimeService().removeVariable(procInstID, key));
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);
    }

    @Override
    protected WorkflowResult<Pair<UserUR, Boolean>> doConfirmPasswordReset(
            final User user, final String token, final String password) {

        Map<String, Object> variables = new HashMap<>(5);
        variables.put(FlowableRuntimeUtils.TOKEN, token);
        variables.put(FlowableRuntimeUtils.PASSWORD, password);
        variables.put(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));
        variables.put(FlowableRuntimeUtils.TASK, "confirmPasswordReset");
        variables.put(FlowableRuntimeUtils.EVENT, "confirmPasswordReset");

        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Set<String> tasks = doExecuteNextTask(procInstID, user, variables);

        userDAO.save(user);

        variables.keySet().forEach(key -> engine.getRuntimeService().removeVariable(procInstID, key));
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);

        PropagationByResource propByRes = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE);
        UserUR updatedPatch = engine.getRuntimeService().getVariable(procInstID, FlowableRuntimeUtils.USER_UR,
                UserUR.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER_UR);
        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE, Boolean.class);
        engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROPAGATE_ENABLE);

        return new WorkflowResult<>(Pair.of(updatedPatch, propagateEnable), propByRes, tasks);
    }

    @Override
    protected void doDelete(final User user) {
        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        doExecuteNextTask(procInstID, user, Collections.singletonMap(FlowableRuntimeUtils.TASK, "delete"));

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.DELETE, userDAO.findAllResourceKeys(user.getKey()));

        if (engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInstID).active().list().isEmpty()) {

            userDAO.delete(user.getKey());

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
                    propByRes);

            FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
            userDAO.save(user);

            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.TASK);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.WF_EXECUTOR);
        }
    }

    @Override
    public WorkflowResult<String> executeNextTask(final WorkflowTaskExecInput workflowTaskExecInput) {
        User user = userDAO.authFind(workflowTaskExecInput.getUserKey());

        String procInstID = FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey());

        Map<String, Object> variables = new HashMap<>();
        variables.put(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));
        variables.putAll(workflowTaskExecInput.getVariables());

        Set<String> performedTasks = doExecuteNextTask(procInstID, user, variables);
        FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        user = userDAO.save(user);

        engine.getRuntimeService().setVariable(procInstID, FlowableRuntimeUtils.USER_TO, dataBinder.
                getUserTO(user, true));

        if (engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInstID).active().list().isEmpty()) {

            userDAO.delete(user.getKey());

            if (!engine.getHistoryService().createHistoricProcessInstanceQuery().
                    processInstanceId(procInstID).list().isEmpty()) {

                engine.getHistoryService().deleteHistoricProcessInstance(procInstID);
            }
        } else {
            PropagationByResource propByRes = engine.getRuntimeService().
                    getVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);

            FlowableRuntimeUtils.saveForFormSubmit(
                    engine,
                    procInstID,
                    user,
                    dataBinder.getUserTO(user, true),
                    null,
                    null,
                    propByRes);
        }

        return new WorkflowResult<>(user.getKey(), null, performedTasks);
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
            Task currentTask = FlowableRuntimeUtils.createTaskQuery(engine, false).
                    processInstanceId(procInstID).singleResult();

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
