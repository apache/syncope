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
import org.apache.syncope.common.lib.to.UserRequestTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.UserRequestFormProperty;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UserRequestFormPropertyType;
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
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.FormType;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.persistence.entity.HistoricFormPropertyEntity;
import org.flowable.engine.runtime.ProcessInstance;
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

    protected User lazyLoad(final User user) {
        // using BeanUtils to access all user's properties and trigger lazy loading - we are about to
        // serialize a User instance for availability within workflow tasks, and this breaks transactions
        BeanUtils.copyProperties(user, entityFactory.newEntity(User.class));
        return user;
    }

    @Override
    public UserRequestTO start(final String bpmnProcess, final User user) {
        Map<String, Object> variables = new HashMap<>(2);
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

        UserRequestTO userRequestTO = new UserRequestTO();
        userRequestTO.setProcessInstanceId(procInst.getProcessInstanceId());
        userRequestTO.setExecutionId(procInst.getId());
        userRequestTO.setBpmnProcess(bpmnProcess);
        userRequestTO.setUser(user.getKey());
        return userRequestTO;
    }

    @Override
    public Pair<ProcessInstance, String> parse(final String executionId) {
        ProcessInstance procInst = null;
        try {
            procInst = engine.getRuntimeService().
                    createProcessInstanceQuery().processInstanceId(executionId).singleResult();
        } catch (FlowableException e) {
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
            FlowableRuntimeUtils.getProcessInstances(engine, event.getAnyKey()).
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

                case "string":
                default:
                    break;
            }
        }

        return result;
    }

    protected UserRequestForm getFormTO(final Task task) {
        return getFormTO(task, engine.getFormService().getTaskFormData(task.getId()));
    }

    protected UserRequestForm getFormTO(final Task task, final TaskFormData fd) {
        UserRequestForm formTO =
                getFormTO(task.getProcessInstanceId(), task.getId(), fd.getFormKey(), fd.getFormProperties());
        BeanUtils.copyProperties(task, formTO);

        return formTO;
    }

    protected UserRequestForm getFormTO(final HistoricTaskInstance task) {
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
            final String procInstID,
            final String taskId,
            final String formKey,
            final List<HistoricFormPropertyEntity> props) {

        UserRequestForm formTO = new UserRequestForm();

        User user = userDAO.find(getUserKey(procInstID));
        if (user == null) {
            throw new NotFoundException("User for process instance id " + procInstID);
        }
        formTO.setUsername(user.getUsername());

        formTO.setTaskId(taskId);
        formTO.setFormKey(formKey);

        formTO.setUserTO(engine.getRuntimeService().
                getVariable(procInstID, FlowableRuntimeUtils.USER_TO, UserTO.class));
        formTO.setUserPatch(engine.getRuntimeService().
                getVariable(procInstID, FlowableRuntimeUtils.USER_PATCH, UserPatch.class));

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
    protected UserRequestForm getFormTO(
            final String procInstID,
            final String taskId,
            final String formKey,
            final List<FormProperty> props) {

        UserRequestForm formTO = new UserRequestForm();

        User user = userDAO.find(getUserKey(procInstID));
        if (user == null) {
            throw new NotFoundException("User for process instance id " + procInstID);
        }
        formTO.setUsername(user.getUsername());

        formTO.setTaskId(taskId);
        formTO.setFormKey(formKey);

        formTO.setUserTO(engine.getRuntimeService().
                getVariable(procInstID, FlowableRuntimeUtils.USER_TO, UserTO.class));
        formTO.setUserPatch(engine.getRuntimeService().
                getVariable(procInstID, FlowableRuntimeUtils.USER_PATCH, UserPatch.class));

        formTO.getProperties().addAll(props.stream().map(fProp -> {
            UserRequestFormProperty propertyTO = new UserRequestFormProperty();
            BeanUtils.copyProperties(fProp, propertyTO, PROPERTY_IGNORE_PROPS);
            propertyTO.setType(fromFlowableFormType(fProp.getType()));
            if (propertyTO.getType() == UserRequestFormPropertyType.Date) {
                propertyTO.setDatePattern((String) fProp.getType().getInformation("datePattern"));
            }
            if (propertyTO.getType() == UserRequestFormPropertyType.Enum) {
                propertyTO.getEnumValues().putAll((Map<String, String>) fProp.getType().getInformation("values"));
            }
            return propertyTO;
        }).collect(Collectors.toList()));

        return formTO;
    }

    @Transactional(readOnly = true)
    @Override
    public Pair<Integer, List<UserRequestForm>> getForms(
            final int page, final int size, final List<OrderByClause> orderByClauses) {

        Pair<Integer, List<UserRequestForm>> forms = null;

        TaskQuery formTaskQuery = FlowableRuntimeUtils.createTaskQuery(engine, true);

        String authUser = AuthContextUtils.getUsername();
        if (adminUser.equals(authUser)) {
            forms = getForms(formTaskQuery, page, size, orderByClauses);
        } else {
            User user = userDAO.findByUsername(authUser);
            if (user == null) {
                throw new NotFoundException("Syncope User " + authUser);
            }

            forms = getForms(formTaskQuery.taskCandidateOrAssigned(user.getUsername()), page, size, orderByClauses);

            List<String> candidateGroups = new ArrayList<>(userDAO.findAllGroupNames(user));
            if (!candidateGroups.isEmpty()) {
                forms = getForms(formTaskQuery.taskCandidateGroupIn(candidateGroups), page, size, orderByClauses);
            }
        }

        return forms == null
                ? Pair.of(0, Collections.<UserRequestForm>emptyList())
                : forms;
    }

    protected Pair<Integer, List<UserRequestForm>> getForms(
            final TaskQuery query, final int page, final int size, final List<OrderByClause> orderByClauses) {

        TaskQuery sortedQuery = query;
        for (OrderByClause clause : orderByClauses) {
            boolean ack = true;
            switch (clause.getField().trim()) {
                case "taskId":
                    sortedQuery = sortedQuery.orderByTaskId();
                    break;

                case "createTime":
                    sortedQuery = sortedQuery.orderByTaskCreateTime();
                    break;

                case "dueDate":
                    sortedQuery = sortedQuery.orderByTaskDueDate();
                    break;

                case "owner":
                    sortedQuery = sortedQuery.orderByTaskOwner();
                    break;

                default:
                    LOG.warn("Form sort request by {}: unsupported, ignoring", clause.getField().trim());
                    ack = false;
            }
            if (ack) {
                sortedQuery = clause.getDirection() == OrderByClause.Direction.ASC
                        ? sortedQuery.asc()
                        : sortedQuery.desc();
            }
        }

        List<UserRequestForm> result = sortedQuery.listPage(size * (page <= 0 ? 0 : page - 1), size).stream().
                map(task -> task instanceof HistoricTaskInstance
                ? getFormTO((HistoricTaskInstance) task) : getFormTO(task)).
                collect(Collectors.toList());

        return Pair.of((int) query.count(), result);
    }

    @Override
    public List<UserRequestForm> getForms(final String userKey) {
        List<UserRequestForm> result = new ArrayList<>();
        FlowableRuntimeUtils.getProcessInstances(engine, userKey).forEach(procInst -> {
            Task task;
            try {
                task = FlowableRuntimeUtils.createTaskQuery(engine, true).
                        processInstanceId(procInst.getProcessInstanceId()).singleResult();
            } catch (FlowableException e) {
                throw new WorkflowException("While reading form for process instance "
                        + procInst.getProcessInstanceId(), e);
            }

            if (task != null) {
                TaskFormData formData;
                try {
                    formData = engine.getFormService().getTaskFormData(task.getId());
                } catch (FlowableException e) {
                    throw new WorkflowException("Error while getting form data for task " + task.getId(), e);
                }
                if (formData != null && !formData.getFormProperties().isEmpty()) {
                    result.add(getFormTO(task, formData));
                }
            }
        });

        return result;
    }

    protected Pair<Task, TaskFormData> checkTask(final String taskId, final String authUser) {
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

        if (!adminUser.equals(authUser)) {
            User user = userDAO.findByUsername(authUser);
            if (user == null) {
                throw new NotFoundException("Syncope User " + authUser);
            }
        }

        return Pair.of(task, formData);
    }

    @Override
    public UserRequestForm claimForm(final String taskId) {
        String authUser = AuthContextUtils.getUsername();
        Pair<Task, TaskFormData> checked = checkTask(taskId, authUser);

        if (!adminUser.equals(authUser)) {
            List<Task> tasksForUser = FlowableRuntimeUtils.createTaskQuery(engine, true).
                    taskId(taskId).taskCandidateUser(authUser).list();
            if (tasksForUser.isEmpty()) {
                throw new WorkflowException(
                        new IllegalArgumentException(authUser + " is not candidate for task " + taskId));
            }
        }

        Task task;
        try {
            engine.getTaskService().setOwner(taskId, authUser);
            task = FlowableRuntimeUtils.createTaskQuery(engine, true).taskId(taskId).singleResult();
        } catch (FlowableException e) {
            throw new WorkflowException("While reading task " + taskId, e);
        }

        return getFormTO(task, checked.getRight());
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
        String authUser = AuthContextUtils.getUsername();
        Pair<Task, TaskFormData> checked = checkTask(form.getTaskId(), authUser);

        if (!checked.getLeft().getOwner().equals(authUser)) {
            throw new WorkflowException(new IllegalArgumentException("Task " + form.getTaskId() + " assigned to "
                    + checked.getLeft().getOwner() + " but submitted by " + authUser));
        }

        String procInstID = checked.getLeft().getProcessInstanceId();

        User user = userDAO.find(getUserKey(procInstID));
        if (user == null) {
            throw new NotFoundException("User with key " + getUserKey(procInstID));
        }

        Set<String> preTasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInstID, user);

        engine.getRuntimeService().setVariable(procInstID, FlowableRuntimeUtils.TASK, "submit");
        engine.getRuntimeService().setVariable(procInstID, FlowableRuntimeUtils.FORM_SUBMITTER, authUser);
        engine.getRuntimeService().setVariable(procInstID, FlowableRuntimeUtils.USER, lazyLoad(user));
        try {
            engine.getFormService().submitTaskFormData(form.getTaskId(), getPropertiesForSubmit(form));
        } catch (FlowableException e) {
            FlowableRuntimeUtils.throwException(e, "While submitting form for task " + form.getTaskId());
        }
        Set<String> postTasks = FlowableRuntimeUtils.getPerformedTasks(engine, procInstID, user);
        postTasks.removeAll(preTasks);
        postTasks.add(form.getTaskId());
        if (procInstID.equals(FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey()))) {
            FlowableRuntimeUtils.updateStatus(engine, procInstID, user);
        }

        user = userDAO.save(user);

        UserPatch userPatch = null;
        String clearPassword = null;
        PropagationByResource propByRes = null;

        ProcessInstance afterSubmitPI = engine.getRuntimeService().
                createProcessInstanceQuery().processInstanceId(procInstID).singleResult();
        if (afterSubmitPI != null) {
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.TASK);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.FORM_SUBMITTER);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER_TO);

            // see if there is any propagation to be done
            propByRes = engine.getRuntimeService().
                    getVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, PropagationByResource.class);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE);

            // fetch - if available - the encrypted password
            String encryptedPwd = engine.getRuntimeService().
                    getVariable(procInstID, FlowableRuntimeUtils.ENCRYPTED_PWD, String.class);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.ENCRYPTED_PWD);
            if (StringUtils.isNotBlank(encryptedPwd)) {
                clearPassword = FlowableRuntimeUtils.decrypt(encryptedPwd);
            }

            Boolean enabled = engine.getRuntimeService().
                    getVariable(procInstID, FlowableRuntimeUtils.ENABLED, Boolean.class);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.ENABLED);

            // supports approval chains
            FlowableRuntimeUtils.saveForFormSubmit(
                    engine,
                    procInstID,
                    user,
                    dataBinder.getUserTO(user, true),
                    clearPassword,
                    enabled,
                    propByRes);

            userPatch = engine.getRuntimeService().
                    getVariable(procInstID, FlowableRuntimeUtils.USER_PATCH, UserPatch.class);
            engine.getRuntimeService().removeVariable(procInstID, FlowableRuntimeUtils.USER_PATCH);
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
