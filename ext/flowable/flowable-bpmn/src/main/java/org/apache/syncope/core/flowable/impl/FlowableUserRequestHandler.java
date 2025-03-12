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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.form.FormProperty;
import org.apache.syncope.common.lib.form.FormPropertyType;
import org.apache.syncope.common.lib.form.FormPropertyValue;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.flowable.api.DropdownValueProvider;
import org.apache.syncope.core.flowable.api.UserRequestHandler;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.form.FormType;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricFormPropertyEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = { Throwable.class })
public class FlowableUserRequestHandler implements UserRequestHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(UserRequestHandler.class);

    protected final UserDataBinder dataBinder;

    protected final String adminUser;

    protected final DomainProcessEngine engine;

    protected final UserDAO userDAO;

    protected final EntityFactory entityFactory;

    public FlowableUserRequestHandler(
            final UserDataBinder dataBinder,
            final String adminUser,
            final DomainProcessEngine engine,
            final UserDAO userDAO,
            final EntityFactory entityFactory) {

        this.dataBinder = dataBinder;
        this.adminUser = adminUser;
        this.engine = engine;
        this.userDAO = userDAO;
        this.entityFactory = entityFactory;
    }

    protected StringBuilder createProcessInstanceQuery(final String userKey) {
        StringBuilder query = new StringBuilder().
                append("SELECT DISTINCT ID_,BUSINESS_KEY_,PROC_DEF_ID_,PROC_INST_ID_,START_TIME_ FROM ").
                append(engine.getManagementService().getTableName(ExecutionEntity.class)).
                append(" WHERE BUSINESS_KEY_ NOT LIKE '").
                append(FlowableRuntimeUtils.getProcBusinessKey(FlowableRuntimeUtils.WF_PROCESS_ID, "%")).
                append('\'');
        if (userKey != null) {
            query.append(" AND BUSINESS_KEY_ LIKE '").
                    append(FlowableRuntimeUtils.getProcBusinessKey("%", userKey)).
                    append('\'');
        }
        query.append(" AND PARENT_ID_ IS NULL");

        return query;
    }

    protected long countProcessInstances(final StringBuilder processInstanceQuery) {
        return engine.getRuntimeService().createNativeProcessInstanceQuery().
                sql("SELECT COUNT(ID_) FROM "
                        + StringUtils.substringAfter(processInstanceQuery.toString(), " FROM ")).
                count();
    }

    protected UserRequest getUserRequest(final ProcessInstance procInst) {
        Pair<String, String> split = FlowableRuntimeUtils.splitProcBusinessKey(procInst.getBusinessKey());

        UserRequest userRequest = new UserRequest();
        userRequest.setBpmnProcess(split.getLeft());
        userRequest.setStartTime(procInst.getStartTime());
        userDAO.findUsername(split.getRight()).ifPresent(userRequest::setUsername);
        userRequest.setExecutionId(procInst.getId());
        Task task = engine.getTaskService().createTaskQuery()
                .processInstanceId(procInst.getProcessInstanceId()).singleResult();
        userRequest.setActivityId(task.getTaskDefinitionKey());
        userRequest.setTaskId(task.getId());
        userRequest.setHasForm(StringUtils.isNotBlank(task.getFormKey()));
        return userRequest;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserRequest> getUserRequests(final String userKey, final Pageable pageable) {
        StringBuilder query = createProcessInstanceQuery(userKey);
        long count = countProcessInstances(query);

        if (!pageable.getSort().isEmpty()) {
            query.append(" ORDER BY");

            for (Sort.Order clause : pageable.getSort()) {
                boolean sorted = true;
                switch (clause.getProperty().trim()) {
                    case "bpmnProcess" ->
                        query.append(" PROC_DEF_ID_");

                    case "startTime" ->
                        query.append(" START_TIME_");

                    case "executionId" ->
                        query.append(" PROC_INST_ID_");

                    default -> {
                        LOG.warn("User request sort request by {}: unsupported, ignoring", clause.getProperty().trim());
                        sorted = false;
                    }
                }
                if (sorted) {
                    if (clause.getDirection() == Sort.Direction.ASC) {
                        query.append(" ASC,");
                    } else {
                        query.append(" DESC,");
                    }
                }
            }

            query.setLength(query.length() - 1);
        }

        List<UserRequest> result = engine.getRuntimeService().createNativeProcessInstanceQuery().
                sql(query.toString()).listPage(
                pageable.getPageSize() * pageable.getPageNumber(),
                pageable.getPageSize()).stream().
                map(this::getUserRequest).
                toList();

        return new SyncopePage<>(result, pageable, count);
    }

    protected User lazyLoad(final User user) {
        // using BeanUtils to access all user's properties and trigger lazy loading - we are about to
        // serialize a User instance for availability within workflow tasks, and this breaks transactions
        BeanUtils.copyProperties(user, entityFactory.newEntity(User.class));
        return user;
    }

    @Override
    public UserRequest start(final String bpmnProcess, final User user, final WorkflowTaskExecInput inputVariables) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(FlowableRuntimeUtils.WF_EXECUTOR, AuthContextUtils.getUsername());
        variables.put(FlowableRuntimeUtils.USER, lazyLoad(user));
        variables.put(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));
        if (inputVariables != null) {
            variables.putAll(inputVariables.getVariables());
        }
        ProcessInstance procInst = null;
        try {
            procInst = engine.getRuntimeService().startProcessInstanceByKey(bpmnProcess, variables);
        } catch (FlowableException e) {
            FlowableRuntimeUtils.throwException(e, "While starting " + bpmnProcess + " instance");
        }

        engine.getRuntimeService().updateBusinessKey(
                Objects.requireNonNull(procInst).getProcessInstanceId(),
                FlowableRuntimeUtils.getProcBusinessKey(bpmnProcess, user.getKey()));

        return getUserRequest(engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInst.getProcessInstanceId()).singleResult());
    }

    @Override
    public Pair<ProcessInstance, String> parse(final String executionId) {
        ProcessInstance procInst = null;
        try {
            procInst = engine.getRuntimeService().
                    createProcessInstanceQuery().processInstanceId(executionId).singleResult();
            if (procInst == null) {
                throw new FlowableIllegalArgumentException("ProcessInstance with id " + executionId);
            }
        } catch (FlowableException e) {
            LOG.error("Could find execution ProcessInstance with id {}", executionId, e);
            throw new NotFoundException("User request execution with id " + executionId);
        }

        return Pair.of(procInst, getUserKey(procInst.getProcessInstanceId()));
    }

    @Override
    public void cancel(final ProcessInstance procInst, final String reason) {
        if (FlowableRuntimeUtils.WF_PROCESS_ID.equals(procInst.getProcessDefinitionKey())) {
            throw new WorkflowException(new IllegalArgumentException(
                    "Cannot cancel a " + FlowableRuntimeUtils.WF_PROCESS_ID + " execution"));
        }

        engine.getRuntimeService().deleteProcessInstance(procInst.getId(), reason);
    }

    @Override
    public void cancelByProcessDefinition(final String processDefinitionId) {
        engine.getRuntimeService().
                createProcessInstanceQuery().processDefinitionId(processDefinitionId).list().
                forEach(procInst -> engine.getRuntimeService().deleteProcessInstance(
                procInst.getId(), "Cascade Delete process definition " + processDefinitionId));
    }

    @Override
    public void cancelByUser(final EntityLifecycleEvent<Entity> event) {
        if (AuthContextUtils.getDomain().equals(event.getDomain())
                && event.getType() == SyncDeltaType.DELETE
                && event.getEntity() instanceof final User user) {

            engine.getRuntimeService().createNativeProcessInstanceQuery().
                    sql(createProcessInstanceQuery(user.getKey()).toString()).
                    list().forEach(procInst -> engine.getRuntimeService().deleteProcessInstance(
                    procInst.getId(), "Cascade Delete user " + user.getUsername()));
        }
    }

    protected static FormPropertyType fromFlowableFormType(final FormType flowableFormType) {
        FormPropertyType result = FormPropertyType.String;

        if (null != flowableFormType.getName()) {
            switch (flowableFormType.getName()) {
                case "long":
                    result = FormPropertyType.Long;
                    break;

                case "enum":
                    result = FormPropertyType.Enum;
                    break;

                case "date":
                    result = FormPropertyType.Date;
                    break;

                case "boolean":
                    result = FormPropertyType.Boolean;
                    break;

                case "dropdown":
                    result = FormPropertyType.Dropdown;
                    break;

                case "password":
                    result = FormPropertyType.Password;
                    break;

                case "string":
                default:
                    break;
            }
        }

        return result;
    }

    protected UserRequestForm getForm(final Task task) {
        return Optional.ofNullable(task).
                map(t -> getForm(t, engine.getFormService().getTaskFormData(t.getId()))).
                orElse(null);
    }

    protected UserRequestForm getForm(final Task task, final TaskFormData fd) {
        UserRequestForm formTO =
                getForm(task.getProcessInstanceId(), task.getId(), fd.getFormKey(), fd.getFormProperties());
        formTO.setCreateTime(task.getCreateTime());
        formTO.setDueDate(task.getDueDate());
        formTO.setExecutionId(task.getExecutionId());
        formTO.setFormKey(task.getFormKey());
        formTO.setAssignee(task.getAssignee());

        return formTO;
    }

    protected UserRequestForm getForm(final HistoricTaskInstance task) {
        List<HistoricFormPropertyEntity> props = engine.getHistoryService().
                createHistoricDetailQuery().taskId(task.getId()).list().stream().
                filter(HistoricFormPropertyEntity.class::isInstance).
                map(HistoricFormPropertyEntity.class::cast).
                toList();

        UserRequestForm formTO = getHistoricFormTO(
                task.getProcessInstanceId(), task.getId(), task.getFormKey(), props);
        formTO.setCreateTime(task.getCreateTime());
        formTO.setDueDate(task.getDueDate());
        formTO.setExecutionId(task.getExecutionId());
        formTO.setFormKey(task.getFormKey());
        formTO.setAssignee(task.getAssignee());

        HistoricActivityInstance historicActivityInstance = engine.getHistoryService().
                createHistoricActivityInstanceQuery().
                executionId(task.getExecutionId()).activityType("userTask").activityName(task.getName()).singleResult();

        if (historicActivityInstance != null) {
            formTO.setCreateTime(historicActivityInstance.getStartTime());
            formTO.setDueDate(historicActivityInstance.getEndTime());
        }

        return formTO;
    }

    protected String getUserKey(final String procInstId) {
        String procBusinessKey = engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInstId).singleResult().getBusinessKey();

        return StringUtils.substringAfter(procBusinessKey, ":");
    }

    protected UserRequestForm getHistoricFormTO(
            final String procInstId,
            final String taskId,
            final String formKey,
            final List<HistoricFormPropertyEntity> props) {

        UserRequestForm formTO = new UserRequestForm();

        formTO.setBpmnProcess(engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInstId).singleResult().getProcessDefinitionKey());

        User user = userDAO.findById(getUserKey(procInstId)).
                orElseThrow(() -> new NotFoundException("User for process instance id " + procInstId));
        formTO.setUsername(user.getUsername());

        formTO.setTaskId(taskId);
        formTO.setFormKey(formKey);

        formTO.setUserTO(engine.getRuntimeService().
                getVariable(procInstId, FlowableRuntimeUtils.USER_TO, UserTO.class));
        formTO.setUserUR(engine.getRuntimeService().
                getVariable(procInstId, FlowableRuntimeUtils.USER_UR, UserUR.class));

        formTO.getProperties().addAll(props.stream().map(prop -> {
            FormProperty propertyTO = new FormProperty();
            propertyTO.setId(prop.getPropertyId());
            propertyTO.setName(prop.getPropertyId());
            propertyTO.setValue(prop.getPropertyValue());
            return propertyTO;
        }).toList());

        return formTO;
    }

    @SuppressWarnings("unchecked")
    protected UserRequestForm getForm(
            final String procInstId,
            final String taskId,
            final String formKey,
            final List<org.flowable.engine.form.FormProperty> props) {

        UserRequestForm formTO = new UserRequestForm();

        formTO.setBpmnProcess(engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInstId).singleResult().getProcessDefinitionKey());

        User user = userDAO.findById(getUserKey(procInstId)).
                orElseThrow(() -> new NotFoundException("User for process instance id " + procInstId));
        formTO.setUsername(user.getUsername());

        formTO.setTaskId(taskId);
        formTO.setFormKey(formKey);

        formTO.setUserTO(engine.getRuntimeService().
                getVariable(procInstId, FlowableRuntimeUtils.USER_TO, UserTO.class));
        formTO.setUserUR(engine.getRuntimeService().
                getVariable(procInstId, FlowableRuntimeUtils.USER_UR, UserUR.class));

        formTO.getProperties().addAll(props.stream().map(fProp -> {
            FormProperty propertyTO = new FormProperty();
            propertyTO.setId(fProp.getId());
            propertyTO.setName(fProp.getName());
            propertyTO.setReadable(fProp.isReadable());
            propertyTO.setRequired(fProp.isRequired());
            propertyTO.setWritable(fProp.isWritable());
            propertyTO.setValue(fProp.getValue());
            propertyTO.setType(fromFlowableFormType(fProp.getType()));
            switch (propertyTO.getType()) {
                case Date ->
                    propertyTO.setDatePattern((String) fProp.getType().getInformation("datePattern"));

                case Enum ->
                    ((Map<String, String>) fProp.getType().getInformation("values")).
                            forEach((key, value) -> propertyTO.getEnumValues().add(new FormPropertyValue(key, value)));

                case Dropdown -> {
                    String valueProviderBean = (String) fProp.getType().getInformation(DropdownValueProvider.NAME);
                    try {
                        DropdownValueProvider valueProvider = ApplicationContextProvider.getApplicationContext().
                                getBean(valueProviderBean, DropdownValueProvider.class);
                        valueProvider.getValues().forEach((key, value) -> propertyTO.getDropdownValues().add(
                                new FormPropertyValue(key, value)));
                    } catch (Exception e) {
                        LOG.error("Could not find bean {} of type {} for form property {}",
                                valueProviderBean, DropdownValueProvider.class.getName(), propertyTO.getId(), e);
                    }
                }

                default -> {
                }
            }
            return propertyTO;
        }).toList());

        return formTO;
    }

    @Override
    public UserRequestForm getForm(final String userKey, final String taskId) {
        TaskQuery query = engine.getTaskService().createTaskQuery().taskId(taskId);
        if (userKey != null) {
            query.processInstanceBusinessKeyLike(FlowableRuntimeUtils.getProcBusinessKey("%", userKey));
        }

        String authUser = AuthContextUtils.getUsername();

        return adminUser.equals(authUser)
                ? getForm(getTask(taskId))
                : getForm(query.or().taskCandidateUser(authUser).taskAssignee(authUser).endOr().singleResult());
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserRequestForm> getForms(final String userKey, final Pageable pageable) {
        TaskQuery query = engine.getTaskService().createTaskQuery().taskWithFormKey();
        if (userKey != null) {
            query.processInstanceBusinessKeyLike(FlowableRuntimeUtils.getProcBusinessKey("%", userKey));
        }

        String authUser = AuthContextUtils.getUsername();
        return adminUser.equals(authUser)
                ? getForms(query, pageable)
                : getForms(query.or().taskCandidateUser(authUser).taskAssignee(authUser).endOr(), pageable);
    }

    protected Page<UserRequestForm> getForms(final TaskQuery query, final Pageable pageable) {
        for (Sort.Order clause : pageable.getSort()) {
            boolean sorted = true;
            switch (clause.getProperty().trim()) {
                case "bpmnProcess":
                    query.orderByProcessDefinitionId();
                    break;

                case "executionId":
                    query.orderByExecutionId();
                    break;

                case "taskId":
                    query.orderByTaskId();
                    break;

                case "createTime":
                    query.orderByTaskCreateTime();
                    break;

                case "dueDate":
                    query.orderByTaskDueDate();
                    break;

                case "assignee":
                    query.orderByTaskAssignee();
                    break;

                default:
                    LOG.warn("Form sort request by {}: unsupported, ignoring", clause.getProperty().trim());
                    sorted = false;
            }
            if (sorted) {
                if (clause.getDirection() == Sort.Direction.ASC) {
                    query.asc();
                } else {
                    query.desc();
                }
            }
        }

        List<UserRequestForm> result = query.listPage(
                pageable.getPageSize() * pageable.getPageNumber(),
                pageable.getPageSize()).stream().
                map(task -> task instanceof final HistoricTaskInstance historicTaskInstance
                ? FlowableUserRequestHandler.this.getForm(historicTaskInstance)
                : FlowableUserRequestHandler.this.getForm(task)).
                toList();

        return new SyncopePage<>(result, pageable, query.count());
    }

    protected Pair<Task, TaskFormData> parseTask(final String taskId) {
        Task task = getTask(taskId);

        TaskFormData formData;
        try {
            formData = engine.getFormService().getTaskFormData(task.getId());
        } catch (FlowableException e) {
            throw new NotFoundException("Form for Flowable Task " + taskId, e);
        }

        return Pair.of(task, formData);
    }

    protected Task getTask(final String taskId) throws NotFoundException {
        Task task;
        try {
            task = engine.getTaskService().createTaskQuery().taskWithFormKey().taskId(taskId).singleResult();
            if (task == null) {
                throw new FlowableException("NULL result");
            }
        } catch (FlowableException e) {
            throw new NotFoundException("Flowable Task " + taskId, e);
        }
        return task;
    }

    @Override
    public UserRequestForm claimForm(final String taskId) {
        Pair<Task, TaskFormData> parsed = parseTask(taskId);

        String authUser = AuthContextUtils.getUsername();
        if (!adminUser.equals(authUser)) {
            List<Task> tasksForUser = engine.getTaskService().createTaskQuery().
                    taskWithFormKey().taskId(taskId).
                    or().taskCandidateUser(authUser).taskAssignee(authUser).endOr().list();
            if (tasksForUser.isEmpty()) {
                throw new WorkflowException(
                        new IllegalArgumentException(authUser + " is not candidate nor assignee of task " + taskId));
            }
        }

        boolean hasAssignees = engine.getTaskService().getIdentityLinksForTask(taskId).stream().
                anyMatch(identityLink -> IdentityLinkType.ASSIGNEE.equals(identityLink.getType()));
        if (hasAssignees) {
            try {
                engine.getTaskService().unclaim(taskId);
            } catch (FlowableException e) {
                throw new WorkflowException("While unclaiming task " + taskId, e);
            }
        }

        Task task;
        try {
            engine.getTaskService().claim(taskId, authUser);
            task = engine.getTaskService().createTaskQuery().taskWithFormKey().taskId(taskId).singleResult();
        } catch (FlowableException e) {
            throw new WorkflowException("While reading task " + taskId, e);
        }

        return FlowableUserRequestHandler.this.getForm(task, parsed.getRight());
    }

    @Override
    public UserRequestForm unclaimForm(final String taskId) {
        Pair<Task, TaskFormData> parsed = parseTask(taskId);

        Task task;
        try {
            engine.getTaskService().unclaim(taskId);
            task = engine.getTaskService().createTaskQuery().taskWithFormKey().taskId(taskId).singleResult();
        } catch (FlowableException e) {
            throw new WorkflowException("While unclaiming task " + taskId, e);
        }

        return FlowableUserRequestHandler.this.getForm(task, parsed.getRight());
    }

    protected Map<String, String> getPropertiesForSubmit(final UserRequestForm form) {
        Map<String, String> props = new HashMap<>();
        form.getProperties().stream().
                filter(FormProperty::isWritable).
                forEach(prop -> props.put(prop.getId(), prop.getValue()));
        return Collections.unmodifiableMap(props);
    }

    protected <T> T getHistoricVariable(
            final List<HistoricVariableInstance> historicVariables, final String name, final Class<T> valueRef) {

        return historicVariables.stream().filter(v -> name.equals(v.getVariableName())).
                findFirst().map(v -> valueRef.cast(v.getValue())).orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public UserWorkflowResult<UserUR> submitForm(final UserRequestForm form) {
        Pair<Task, TaskFormData> parsed = parseTask(form.getTaskId());

        String authUser = AuthContextUtils.getUsername();
        if (!parsed.getLeft().getAssignee().equals(authUser)) {
            throw new WorkflowException(new IllegalArgumentException("Task " + form.getTaskId() + " assigned to "
                    + parsed.getLeft().getAssignee() + " but submitted by " + authUser));
        }

        String procInstId = parsed.getLeft().getProcessInstanceId();

        User user = userDAO.findById(getUserKey(procInstId)).
                orElseThrow(() -> new NotFoundException("User for process instance id " + procInstId));

        Set<String> preTasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInstId);

        engine.getRuntimeService().setVariable(procInstId, FlowableRuntimeUtils.TASK, "submit");
        engine.getRuntimeService().setVariable(procInstId, FlowableRuntimeUtils.FORM_SUBMITTER, authUser);
        engine.getRuntimeService().setVariable(procInstId, FlowableRuntimeUtils.USER, lazyLoad(user));
        try {
            engine.getFormService().submitTaskFormData(form.getTaskId(), getPropertiesForSubmit(form));
        } catch (FlowableException e) {
            FlowableRuntimeUtils.throwException(e, "While submitting form for task " + form.getTaskId());
        }
        Set<String> postTasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInstId);
        postTasks.removeAll(preTasks);
        postTasks.add(form.getTaskId());
        if (procInstId.equals(FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey()))) {
            FlowableRuntimeUtils.updateStatus(engine, procInstId, user);
        }

        user = userDAO.save(user);

        PropagationByResource<String> propByRes;
        PropagationByResource<Pair<String, String>> propByLinkedAccount;
        String clearPassword = null;
        UserUR userUR;
        if (engine.getRuntimeService().
                createProcessInstanceQuery().processInstanceId(procInstId).singleResult() == null) {

            List<HistoricVariableInstance> historicVariables = engine.getHistoryService().
                    createHistoricVariableInstanceQuery().processInstanceId(procInstId).list();

            // see if there is any propagation to be done
            propByRes = getHistoricVariable(
                    historicVariables, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
            propByLinkedAccount = getHistoricVariable(
                    historicVariables, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, PropagationByResource.class);

            // fetch - if available - the encrypted password
            String encryptedPwd = getHistoricVariable(
                    historicVariables, FlowableRuntimeUtils.ENCRYPTED_PWD, String.class);
            if (StringUtils.isNotBlank(encryptedPwd)) {
                clearPassword = FlowableRuntimeUtils.decrypt(encryptedPwd);
            }

            userUR = getHistoricVariable(historicVariables, FlowableRuntimeUtils.USER_UR, UserUR.class);
        } else {
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.TASK);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.FORM_SUBMITTER);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.USER);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.USER_TO);

            // see if there is any propagation to be done
            propByRes = engine.getRuntimeService().getVariable(
                    procInstId, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.PROP_BY_RESOURCE);
            propByLinkedAccount = engine.getRuntimeService().getVariable(
                    procInstId, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, PropagationByResource.class);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT);

            // fetch - if available - the encrypted password
            String encryptedPwd = engine.getRuntimeService().getVariable(
                    procInstId, FlowableRuntimeUtils.ENCRYPTED_PWD, String.class);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.ENCRYPTED_PWD);
            if (StringUtils.isNotBlank(encryptedPwd)) {
                clearPassword = FlowableRuntimeUtils.decrypt(encryptedPwd);
            }

            Boolean enabled = engine.getRuntimeService().getVariable(
                    procInstId, FlowableRuntimeUtils.ENABLED, Boolean.class);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.ENABLED);

            // supports approval chains
            FlowableRuntimeUtils.saveForFormSubmit(
                    engine,
                    procInstId,
                    dataBinder.getUserTO(user, true),
                    clearPassword,
                    enabled,
                    propByRes,
                    propByLinkedAccount);

            userUR = engine.getRuntimeService().getVariable(
                    procInstId, FlowableRuntimeUtils.USER_UR, UserUR.class);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.USER_UR);
        }

        if (userUR == null) {
            userUR = new UserUR();
            userUR.setKey(user.getKey());
            userUR.setPassword(new PasswordPatch.Builder().onSyncope(true).value(clearPassword).build());

            Set<String> pwdResources = new HashSet<>();
            if (propByRes != null) {
                pwdResources.addAll(propByRes.get(ResourceOperation.CREATE));
            }
            if (propByLinkedAccount != null) {
                pwdResources.addAll(propByLinkedAccount.get(ResourceOperation.CREATE).stream().
                        map(Pair::getLeft).toList());
            }
            userUR.getPassword().getResources().addAll(pwdResources);
        }

        return new UserWorkflowResult<>(userUR, propByRes, propByLinkedAccount, postTasks);
    }
}
