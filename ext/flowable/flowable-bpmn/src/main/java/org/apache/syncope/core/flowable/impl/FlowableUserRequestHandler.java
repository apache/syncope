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

import org.apache.syncope.core.flowable.api.UserRequestHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.UserRequestFormProperty;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UserRequestFormPropertyType;
import org.apache.syncope.core.flowable.api.DropdownValueProvider;
import org.apache.syncope.core.flowable.api.WorkflowTaskManager;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.event.AnyDeletedEvent;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.FormType;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricFormPropertyEntity;
import org.flowable.engine.runtime.NativeProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = { Throwable.class })
public class FlowableUserRequestHandler implements UserRequestHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(UserRequestHandler.class);

    protected static final String[] PROPERTY_IGNORE_PROPS = { "type" };

    @Autowired
    protected WorkflowTaskManager wfTaskManager;

    @Autowired
    protected UserDataBinder dataBinder;

    @Resource(name = "adminUser")
    protected String adminUser;

    @Autowired
    protected DomainProcessEngine engine;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected EntityFactory entityFactory;

    protected NativeProcessInstanceQuery createProcessInstanceQuery(final String userKey) {
        return engine.getRuntimeService().createNativeProcessInstanceQuery().
                sql("SELECT DISTINCT ID_,BUSINESS_KEY_,ACT_ID_ FROM "
                        + engine.getManagementService().getTableName(ExecutionEntity.class)
                        + " WHERE BUSINESS_KEY_ LIKE '"
                        + FlowableRuntimeUtils.getProcBusinessKey("%", userKey) + "'"
                        + " AND BUSINESS_KEY_ NOT LIKE '"
                        + FlowableRuntimeUtils.getProcBusinessKey(FlowableRuntimeUtils.WF_PROCESS_ID, "%") + "'"
                        + " AND PARENT_ID_ IS NULL");
    }

    protected int countProcessInstances(final String userKey) {
        return (int) engine.getRuntimeService().createNativeProcessInstanceQuery().
                sql("SELECT COUNT(ID_) FROM "
                        + engine.getManagementService().getTableName(ExecutionEntity.class)
                        + " WHERE BUSINESS_KEY_ LIKE '"
                        + FlowableRuntimeUtils.getProcBusinessKey("%", userKey) + "'"
                        + " AND BUSINESS_KEY_ NOT LIKE '"
                        + FlowableRuntimeUtils.getProcBusinessKey(FlowableRuntimeUtils.WF_PROCESS_ID, "%") + "'"
                        + " AND PARENT_ID_ IS NULL").count();
    }

    protected UserRequest getUserRequest(final ProcessInstance procInst) {
        Pair<String, String> split = FlowableRuntimeUtils.splitProcBusinessKey(procInst.getBusinessKey());

        UserRequest userRequest = new UserRequest();
        userRequest.setBpmnProcess(split.getLeft());
        userRequest.setUser(split.getRight());
        userRequest.setExecutionId(procInst.getId());
        userRequest.setActivityId(procInst.getActivityId());
        return userRequest;
    }

    @Transactional(readOnly = true)
    @Override
    public Pair<Integer, List<UserRequest>> getUserRequests(
            final String userKey,
            final int page,
            final int size,
            final List<OrderByClause> orderByClauses) {

        Integer count = null;
        List<UserRequest> result = null;
        if (userKey == null) {
            ProcessInstanceQuery query = engine.getRuntimeService().createProcessInstanceQuery().active();
            for (OrderByClause clause : orderByClauses) {
                boolean sorted = true;
                switch (clause.getField().trim()) {
                    case "processDefinitionId":
                        query.orderByProcessDefinitionId();
                        break;

                    case "processDefinitionKey":
                        query.orderByProcessDefinitionKey();
                        break;

                    case "processInstanceId":
                        query.orderByProcessInstanceId();
                        break;

                    default:
                        LOG.warn("User request sort request by {}: unsupported, ignoring", clause.getField().trim());
                        sorted = false;
                }
                if (sorted) {
                    if (clause.getDirection() == OrderByClause.Direction.ASC) {
                        query.asc();
                    } else {
                        query.desc();
                    }
                }

                count = (int) query.count();
                result = query.listPage(size * (page <= 0 ? 0 : page - 1), size).stream().
                        map(procInst -> getUserRequest(procInst)).
                        collect(Collectors.toList());
            }
        } else {
            count = countProcessInstances(userKey);
            result = createProcessInstanceQuery(userKey).listPage(size * (page <= 0 ? 0 : page - 1), size).stream().
                    map(procInst -> getUserRequest(procInst)).
                    collect(Collectors.toList());
        }

        return Pair.of(count, result);
    }

    protected User lazyLoad(final User user) {
        // using BeanUtils to access all user's properties and trigger lazy loading - we are about to
        // serialize a User instance for availability within workflow tasks, and this breaks transactions
        BeanUtils.copyProperties(user, entityFactory.newEntity(User.class));
        return user;
    }

    @Override
    public UserRequest start(final String bpmnProcess, final User user) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(FlowableRuntimeUtils.WF_EXECUTOR, AuthContextUtils.getUsername());
        variables.put(FlowableRuntimeUtils.USER, lazyLoad(user));
        variables.put(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));

        ProcessInstance procInst = null;
        try {
            procInst = engine.getRuntimeService().startProcessInstanceByKey(bpmnProcess, variables);
        } catch (FlowableException e) {
            FlowableRuntimeUtils.throwException(e, "While starting " + bpmnProcess + " instance");
        }

        engine.getRuntimeService().updateBusinessKey(
                procInst.getProcessInstanceId(),
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
                forEach(procInst -> {
                    engine.getRuntimeService().deleteProcessInstance(
                            procInst.getId(), "Cascade Delete process definition " + processDefinitionId);
                });
    }

    @Override
    public void cancelByUser(final AnyDeletedEvent event) {
        if (AuthContextUtils.getDomain().equals(event.getDomain()) && event.getAnyTypeKind() == AnyTypeKind.USER) {
            String username = event.getAnyName();
            createProcessInstanceQuery(event.getAnyKey()).list().
                    forEach(procInst -> {
                        engine.getRuntimeService().deleteProcessInstance(
                                procInst.getId(), "Cascade Delete user " + username);
                    });
        }
    }

    protected UserRequestFormPropertyType fromFlowableFormType(final FormType flowableFormType) {
        UserRequestFormPropertyType result = UserRequestFormPropertyType.String;

        if (null != flowableFormType.getName()) {
            switch (flowableFormType.getName()) {
                case "long":
                    result = UserRequestFormPropertyType.Long;
                    break;

                case "enum":
                    result = UserRequestFormPropertyType.Enum;
                    break;

                case "date":
                    result = UserRequestFormPropertyType.Date;
                    break;

                case "boolean":
                    result = UserRequestFormPropertyType.Boolean;
                    break;

                case "dropdown":
                    result = UserRequestFormPropertyType.Dropdown;
                    break;

                case "string":
                default:
                    break;
            }
        }

        return result;
    }

    protected UserRequestForm getForm(final Task task) {
        return FlowableUserRequestHandler.this.getForm(task, engine.getFormService().getTaskFormData(task.getId()));
    }

    protected UserRequestForm getForm(final Task task, final TaskFormData fd) {
        UserRequestForm formTO =
                getForm(task.getProcessInstanceId(), task.getId(), fd.getFormKey(), fd.getFormProperties());
        BeanUtils.copyProperties(task, formTO);

        return formTO;
    }

    protected UserRequestForm getForm(final HistoricTaskInstance task) {
        List<HistoricFormPropertyEntity> props = engine.getHistoryService().
                createHistoricDetailQuery().taskId(task.getId()).list().stream().
                filter(HistoricFormPropertyEntity.class::isInstance).
                map(HistoricFormPropertyEntity.class::cast).
                collect(Collectors.toList());

        UserRequestForm formTO = getHistoricFormTO(
                task.getProcessInstanceId(), task.getId(), task.getFormKey(), props);
        BeanUtils.copyProperties(task, formTO);

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

        User user = userDAO.find(getUserKey(procInstId));
        if (user == null) {
            throw new NotFoundException("User for process instance id " + procInstId);
        }
        formTO.setUsername(user.getUsername());

        formTO.setTaskId(taskId);
        formTO.setFormKey(formKey);

        formTO.setUserTO(engine.getRuntimeService().
                getVariable(procInstId, FlowableRuntimeUtils.USER_TO, UserTO.class));
        formTO.setUserPatch(engine.getRuntimeService().
                getVariable(procInstId, FlowableRuntimeUtils.USER_PATCH, UserPatch.class));

        formTO.getProperties().addAll(props.stream().map(prop -> {
            UserRequestFormProperty propertyTO = new UserRequestFormProperty();
            propertyTO.setId(prop.getPropertyId());
            propertyTO.setName(prop.getPropertyId());
            propertyTO.setValue(prop.getPropertyValue());
            return propertyTO;
        }).collect(Collectors.toList()));

        return formTO;
    }

    @SuppressWarnings("unchecked")
    protected UserRequestForm getForm(
            final String procInstId,
            final String taskId,
            final String formKey,
            final List<FormProperty> props) {

        UserRequestForm formTO = new UserRequestForm();

        formTO.setBpmnProcess(engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(procInstId).singleResult().getProcessDefinitionKey());

        User user = userDAO.find(getUserKey(procInstId));
        if (user == null) {
            throw new NotFoundException("User for process instance id " + procInstId);
        }
        formTO.setUsername(user.getUsername());

        formTO.setExecutionId(procInstId);
        formTO.setTaskId(taskId);
        formTO.setFormKey(formKey);

        formTO.setUserTO(engine.getRuntimeService().
                getVariable(procInstId, FlowableRuntimeUtils.USER_TO, UserTO.class));
        formTO.setUserPatch(engine.getRuntimeService().
                getVariable(procInstId, FlowableRuntimeUtils.USER_PATCH, UserPatch.class));

        formTO.getProperties().addAll(props.stream().map(fProp -> {
            UserRequestFormProperty propertyTO = new UserRequestFormProperty();
            BeanUtils.copyProperties(fProp, propertyTO, PROPERTY_IGNORE_PROPS);
            propertyTO.setType(fromFlowableFormType(fProp.getType()));
            switch (propertyTO.getType()) {
                case Date:
                    propertyTO.setDatePattern((String) fProp.getType().getInformation("datePattern"));
                    break;

                case Enum:
                    propertyTO.getEnumValues().putAll((Map<String, String>) fProp.getType().getInformation("values"));
                    break;

                case Dropdown:
                    String valueProviderBean = (String) fProp.getType().getInformation(DropdownValueProvider.NAME);
                    try {
                        DropdownValueProvider valueProvider = ApplicationContextProvider.getApplicationContext().
                                getBean(valueProviderBean, DropdownValueProvider.class);
                        propertyTO.getDropdownValues().putAll(valueProvider.getValues());
                    } catch (Exception e) {
                        LOG.error("Could not find bean {} of type {} for form property {}",
                                valueProviderBean, DropdownValueProvider.class.getName(), propertyTO.getId(), e);
                    }
                    break;

                default:
            }
            return propertyTO;
        }).collect(Collectors.toList()));

        return formTO;
    }

    @Transactional(readOnly = true)
    @Override
    public Pair<Integer, List<UserRequestForm>> getForms(
            final String userKey,
            final int page,
            final int size,
            final List<OrderByClause> orderByClauses) {

        Pair<Integer, List<UserRequestForm>> forms;

        TaskQuery query = FlowableRuntimeUtils.createTaskQuery(engine, true);
        if (userKey != null) {
            query.processInstanceBusinessKeyLike(FlowableRuntimeUtils.getProcBusinessKey("%", userKey));
        }

        String authUser = AuthContextUtils.getUsername();
        if (adminUser.equals(authUser)) {
            forms = getForms(query, page, size, orderByClauses);
        } else {
            User user = userDAO.findByUsername(authUser);
            forms = getForms(query.taskCandidateOrAssigned(user.getUsername()), page, size, orderByClauses);

            List<String> candidateGroups = new ArrayList<>(userDAO.findAllGroupNames(user));
            if (!candidateGroups.isEmpty()) {
                forms = getForms(query.taskCandidateGroupIn(candidateGroups), page, size, orderByClauses);
            }
        }

        return forms == null
                ? Pair.of(0, Collections.<UserRequestForm>emptyList())
                : forms;
    }

    protected Pair<Integer, List<UserRequestForm>> getForms(
            final TaskQuery query, final int page, final int size, final List<OrderByClause> orderByClauses) {

        for (OrderByClause clause : orderByClauses) {
            boolean sorted = true;
            switch (clause.getField().trim()) {
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

                case "owner":
                    query.orderByTaskOwner();
                    break;

                default:
                    LOG.warn("Form sort request by {}: unsupported, ignoring", clause.getField().trim());
                    sorted = false;
            }
            if (sorted) {
                if (clause.getDirection() == OrderByClause.Direction.ASC) {
                    query.asc();
                } else {
                    query.desc();
                }
            }
        }

        List<UserRequestForm> result = query.listPage(size * (page <= 0 ? 0 : page - 1), size).stream().
                map(task -> task instanceof HistoricTaskInstance
                ? FlowableUserRequestHandler.this.getForm((HistoricTaskInstance) task)
                : FlowableUserRequestHandler.this.getForm(task)).
                collect(Collectors.toList());

        return Pair.of((int) query.count(), result);
    }

    protected Pair<Task, TaskFormData> parseTask(final String taskId) {
        Task task;
        try {
            task = FlowableRuntimeUtils.createTaskQuery(engine, true).taskId(taskId).singleResult();
            if (task == null) {
                throw new FlowableException("NULL result");
            }
        } catch (FlowableException e) {
            throw new NotFoundException("Flowable Task " + taskId, e);
        }

        TaskFormData formData;
        try {
            formData = engine.getFormService().getTaskFormData(task.getId());
        } catch (FlowableException e) {
            throw new NotFoundException("Form for Flowable Task " + taskId, e);
        }

        return Pair.of(task, formData);
    }

    @Override
    public UserRequestForm claimForm(final String taskId) {
        Pair<Task, TaskFormData> parsed = parseTask(taskId);

        String authUser = AuthContextUtils.getUsername();
        if (!adminUser.equals(authUser)) {
            List<Task> tasksForUser = FlowableRuntimeUtils.createTaskQuery(engine, true).
                    taskId(taskId).taskCandidateOrAssigned(authUser).list();
            if (tasksForUser.isEmpty()) {
                throw new WorkflowException(
                        new IllegalArgumentException(authUser + " is not candidate nor assignee of task " + taskId));
            }
        }

        Task task;
        try {
            engine.getTaskService().setOwner(taskId, authUser);
            task = FlowableRuntimeUtils.createTaskQuery(engine, true).taskId(taskId).singleResult();
        } catch (FlowableException e) {
            throw new WorkflowException("While reading task " + taskId, e);
        }

        return FlowableUserRequestHandler.this.getForm(task, parsed.getRight());
    }

    private Map<String, String> getPropertiesForSubmit(final UserRequestForm form) {
        Map<String, String> props = new HashMap<>();
        form.getProperties().stream().
                filter(UserRequestFormProperty::isWritable).
                forEach(prop -> {
                    props.put(prop.getId(), prop.getValue());
                });
        return Collections.unmodifiableMap(props);
    }

    @Override
    public WorkflowResult<UserPatch> submitForm(final UserRequestForm form) {
        Pair<Task, TaskFormData> parsed = parseTask(form.getTaskId());

        String authUser = AuthContextUtils.getUsername();
        if (!parsed.getLeft().getOwner().equals(authUser)) {
            throw new WorkflowException(new IllegalArgumentException("Task " + form.getTaskId() + " assigned to "
                    + parsed.getLeft().getOwner() + " but submitted by " + authUser));
        }

        String procInstId = parsed.getLeft().getProcessInstanceId();

        User user = userDAO.find(getUserKey(procInstId));
        if (user == null) {
            throw new NotFoundException("User with key " + getUserKey(procInstId));
        }

        Set<String> preTasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInstId, user);

        engine.getRuntimeService().setVariable(procInstId, FlowableRuntimeUtils.TASK, "submit");
        engine.getRuntimeService().setVariable(procInstId, FlowableRuntimeUtils.FORM_SUBMITTER, authUser);
        engine.getRuntimeService().setVariable(procInstId, FlowableRuntimeUtils.USER, lazyLoad(user));
        try {
            engine.getFormService().submitTaskFormData(form.getTaskId(), getPropertiesForSubmit(form));
        } catch (FlowableException e) {
            FlowableRuntimeUtils.throwException(e, "While submitting form for task " + form.getTaskId());
        }
        Set<String> postTasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInstId, user);
        postTasks.removeAll(preTasks);
        postTasks.add(form.getTaskId());
        if (procInstId.equals(FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey()))) {
            FlowableRuntimeUtils.updateStatus(engine, procInstId, user);
        }

        user = userDAO.save(user);

        UserPatch userPatch = null;
        String clearPassword = null;
        PropagationByResource propByRes = null;

        ProcessInstance afterSubmitPI = engine.getRuntimeService().
                createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        if (afterSubmitPI != null) {
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.TASK);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.FORM_SUBMITTER);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.USER);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.USER_TO);

            // see if there is any propagation to be done
            propByRes = engine.getRuntimeService().
                    getVariable(procInstId, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.PROP_BY_RESOURCE);

            // fetch - if available - the encrypted password
            String encryptedPwd = engine.getRuntimeService().
                    getVariable(procInstId, FlowableRuntimeUtils.ENCRYPTED_PWD, String.class);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.ENCRYPTED_PWD);
            if (StringUtils.isNotBlank(encryptedPwd)) {
                clearPassword = FlowableRuntimeUtils.decrypt(encryptedPwd);
            }

            Boolean enabled = engine.getRuntimeService().
                    getVariable(procInstId, FlowableRuntimeUtils.ENABLED, Boolean.class);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.ENABLED);

            // supports approval chains
            FlowableRuntimeUtils.saveForFormSubmit(
                    engine,
                    procInstId,
                    user,
                    dataBinder.getUserTO(user, true),
                    clearPassword,
                    enabled,
                    propByRes);

            userPatch = engine.getRuntimeService().
                    getVariable(procInstId, FlowableRuntimeUtils.USER_PATCH, UserPatch.class);
            engine.getRuntimeService().removeVariable(procInstId, FlowableRuntimeUtils.USER_PATCH);
        }
        if (userPatch == null) {
            userPatch = new UserPatch();
            userPatch.setKey(user.getKey());
            userPatch.setPassword(new PasswordPatch.Builder().onSyncope(true).value(clearPassword).build());

            if (propByRes != null) {
                userPatch.getPassword().getResources().addAll(propByRes.get(ResourceOperation.CREATE));
            }
        }

        return new WorkflowResult<>(userPatch, propByRes, postTasks);
    }
}
