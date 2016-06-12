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
package org.apache.syncope.core.workflow.activiti;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import javax.ws.rs.NotFoundException;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.FormType;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.persistence.entity.HistoricFormPropertyEntity;
import org.activiti.engine.query.Query;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormPropertyTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.WorkflowFormPropertyType;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.workflow.activiti.spring.DomainProcessEngine;
import org.apache.syncope.core.workflow.api.WorkflowDefinitionFormat;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.apache.syncope.core.workflow.java.AbstractUserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * <a href="http://www.activiti.org/">Activiti</a> based implementation.
 */
public class ActivitiUserWorkflowAdapter extends AbstractUserWorkflowAdapter {

    protected static final String[] PROPERTY_IGNORE_PROPS = { "type" };

    public static final String WF_PROCESS_ID = "userWorkflow";

    public static final String WF_PROCESS_RESOURCE = "userWorkflow.bpmn20.xml";

    public static final String WF_DGRM_RESOURCE = "userWorkflow.userWorkflow.png";

    public static final String USER = "user";

    public static final String WF_EXECUTOR = "wfExecutor";

    public static final String FORM_SUBMITTER = "formSubmitter";

    public static final String USER_TO = "userTO";

    public static final String ENABLED = "enabled";

    public static final String USER_PATCH = "userPatch";

    public static final String EMAIL_KIND = "emailKind";

    public static final String TASK = "task";

    public static final String TOKEN = "token";

    public static final String PASSWORD = "password";

    public static final String PROP_BY_RESOURCE = "propByResource";

    public static final String PROPAGATE_ENABLE = "propagateEnable";

    public static final String ENCRYPTED_PWD = "encryptedPwd";

    public static final String TASK_IS_FORM = "taskIsForm";

    public static final String MODEL_DATA_JSON_MODEL = "model";

    public static final String STORE_PASSWORD = "storePassword";

    public static final String EVENT = "event";

    @Resource(name = "adminUser")
    protected String adminUser;

    @Autowired
    protected DomainProcessEngine engine;

    @Override
    public String getPrefix() {
        return "ACT_";
    }

    protected void throwException(final ActivitiException e, final String defaultMessage) {
        if (e.getCause() != null) {
            if (e.getCause().getCause() instanceof SyncopeClientException) {
                throw (SyncopeClientException) e.getCause().getCause();
            } else if (e.getCause().getCause() instanceof ParsingValidationException) {
                throw (ParsingValidationException) e.getCause().getCause();
            } else if (e.getCause().getCause() instanceof InvalidEntityException) {
                throw (InvalidEntityException) e.getCause().getCause();
            }
        }

        throw new WorkflowException(defaultMessage, e);
    }

    protected void updateStatus(final User user) {
        List<Task> tasks = engine.getTaskService().createTaskQuery().processInstanceId(user.getWorkflowId()).list();
        if (tasks.isEmpty() || tasks.size() > 1) {
            LOG.warn("While setting user status: unexpected task number ({})", tasks.size());
        } else {
            user.setStatus(tasks.get(0).getTaskDefinitionKey());
        }
    }

    protected String getFormTask(final User user) {
        String result = null;

        List<Task> tasks = engine.getTaskService().createTaskQuery().processInstanceId(user.getWorkflowId()).list();
        if (tasks.isEmpty() || tasks.size() > 1) {
            LOG.warn("While checking if form task: unexpected task number ({})", tasks.size());
        } else {
            try {
                TaskFormData formData = engine.getFormService().getTaskFormData(tasks.get(0).getId());
                if (formData != null && !formData.getFormProperties().isEmpty()) {
                    result = tasks.get(0).getId();
                }
            } catch (ActivitiException e) {
                LOG.warn("Could not get task form data", e);
            }
        }

        return result;
    }

    protected Set<String> getPerformedTasks(final User user) {
        final Set<String> result = new HashSet<>();

        for (HistoricActivityInstance task : engine.getHistoryService().createHistoricActivityInstanceQuery().
                executionId(user.getWorkflowId()).list()) {

            result.add(task.getActivityId());
        }

        return result;
    }

    /**
     * Saves resources to be propagated and password for later - after form submission - propagation.
     *
     * @param user user
     * @param password pasword
     * @param propByRes current propagation actions against resources
     */
    protected void saveForFormSubmit(final User user, final String password, final PropagationByResource propByRes) {
        String formTaskId = getFormTask(user);
        if (formTaskId != null) {
            // SYNCOPE-238: This is needed to simplify the task query in this.getForms()
            engine.getTaskService().setVariableLocal(formTaskId, TASK_IS_FORM, Boolean.TRUE);
            engine.getRuntimeService().setVariable(user.getWorkflowId(), PROP_BY_RESOURCE, propByRes);
            if (propByRes != null) {
                propByRes.clear();
            }

            if (StringUtils.isNotBlank(password)) {
                engine.getRuntimeService().setVariable(user.getWorkflowId(), ENCRYPTED_PWD, encrypt(password));
            }
        }
    }

    @Override
    public WorkflowResult<Pair<String, Boolean>> create(final UserTO userTO, final boolean disablePwdPolicyCheck,
            final boolean storePassword) {

        return create(userTO, disablePwdPolicyCheck, null, storePassword);
    }

    @Override
    public WorkflowResult<Pair<String, Boolean>> create(final UserTO userTO, final boolean storePassword) {
        return create(userTO, false, storePassword);
    }

    @Override
    public WorkflowResult<Pair<String, Boolean>> create(final UserTO userTO, final boolean disablePwdPolicyCheck,
            final Boolean enabled, final boolean storePassword) {

        Map<String, Object> variables = new HashMap<>();
        variables.put(WF_EXECUTOR, AuthContextUtils.getUsername());
        variables.put(USER_TO, userTO);
        variables.put(ENABLED, enabled);
        variables.put(STORE_PASSWORD, storePassword);

        ProcessInstance processInstance = null;
        try {
            processInstance = engine.getRuntimeService().startProcessInstanceByKey(WF_PROCESS_ID, variables);
        } catch (ActivitiException e) {
            throwException(e, "While starting " + WF_PROCESS_ID + " instance");
        }

        User user = engine.getRuntimeService().getVariable(processInstance.getProcessInstanceId(), USER, User.class);

        Boolean updatedEnabled =
                engine.getRuntimeService().getVariable(processInstance.getProcessInstanceId(), ENABLED, Boolean.class);
        if (updatedEnabled != null) {
            user.setSuspended(!updatedEnabled);
        }

        // this will make UserValidator not to consider password policies at all
        if (disablePwdPolicyCheck) {
            user.removeClearPassword();
        }

        updateStatus(user);
        user = userDAO.save(user);

        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                processInstance.getProcessInstanceId(), PROPAGATE_ENABLE, Boolean.class);
        if (propagateEnable == null) {
            propagateEnable = enabled;
        }

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.CREATE, userDAO.findAllResourceNames(user));

        saveForFormSubmit(user, userTO.getPassword(), propByRes);

        Set<String> tasks = getPerformedTasks(user);

        return new WorkflowResult<Pair<String, Boolean>>(
                new ImmutablePair<>(user.getKey(), propagateEnable), propByRes, tasks);
    }

    protected Set<String> doExecuteTask(final User user, final String task, final Map<String, Object> moreVariables) {
        Set<String> preTasks = getPerformedTasks(user);

        Map<String, Object> variables = new HashMap<>();
        variables.put(WF_EXECUTOR, AuthContextUtils.getUsername());
        variables.put(TASK, task);

        // using BeanUtils to access all user's properties and trigger lazy loading - we are about to
        // serialize a User instance for availability within workflow tasks, and this breaks transactions
        BeanUtils.copyProperties(user, entityFactory.newEntity(User.class));
        variables.put(USER, user);

        if (moreVariables != null && !moreVariables.isEmpty()) {
            variables.putAll(moreVariables);
        }

        if (StringUtils.isBlank(user.getWorkflowId())) {
            throw new WorkflowException(new NotFoundException("Empty workflow id for " + user));
        }

        List<Task> tasks = engine.getTaskService().createTaskQuery().processInstanceId(user.getWorkflowId()).list();
        if (tasks.size() == 1) {
            try {
                engine.getTaskService().complete(tasks.get(0).getId(), variables);
            } catch (ActivitiException e) {
                throwException(e, "While completing task '" + tasks.get(0).getName() + "' for " + user);
            }
        } else {
            LOG.warn("Expected a single task, found {}", tasks.size());
        }

        Set<String> postTasks = getPerformedTasks(user);
        postTasks.removeAll(preTasks);
        postTasks.add(task);

        return postTasks;
    }

    @Override
    protected WorkflowResult<String> doActivate(final User user, final String token) {
        Set<String> tasks = doExecuteTask(user, "activate", Collections.singletonMap(TOKEN, (Object) token));

        updateStatus(user);
        User updated = userDAO.save(user);

        return new WorkflowResult<>(updated.getKey(), null, tasks);
    }

    @Override
    protected WorkflowResult<Pair<UserPatch, Boolean>> doUpdate(final User user, final UserPatch userPatch) {
        Set<String> tasks = doExecuteTask(user, "update", Collections.singletonMap(USER_PATCH, (Object) userPatch));

        updateStatus(user);
        User updated = userDAO.save(user);

        PropagationByResource propByRes = engine.getRuntimeService().getVariable(
                user.getWorkflowId(), PROP_BY_RESOURCE, PropagationByResource.class);
        UserPatch updatedPatch = engine.getRuntimeService().getVariable(
                user.getWorkflowId(), USER_PATCH, UserPatch.class);

        saveForFormSubmit(
                updated, updatedPatch.getPassword() == null ? null : updatedPatch.getPassword().getValue(), propByRes);

        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                user.getWorkflowId(), PROPAGATE_ENABLE, Boolean.class);

        return new WorkflowResult<Pair<UserPatch, Boolean>>(
                new ImmutablePair<>(updatedPatch, propagateEnable), propByRes, tasks);
    }

    @Override
    protected WorkflowResult<String> doSuspend(final User user) {
        Set<String> performedTasks = doExecuteTask(user, "suspend", null);
        updateStatus(user);
        User updated = userDAO.save(user);

        return new WorkflowResult<>(updated.getKey(), null, performedTasks);
    }

    @Override
    protected WorkflowResult<String> doReactivate(final User user) {
        Set<String> performedTasks = doExecuteTask(user, "reactivate", null);
        updateStatus(user);

        User updated = userDAO.save(user);

        return new WorkflowResult<>(updated.getKey(), null, performedTasks);
    }

    @Override
    protected void doRequestPasswordReset(final User user) {
        Map<String, Object> variables = new HashMap<>(2);
        variables.put(USER_TO, dataBinder.getUserTO(user, true));
        variables.put(EVENT, "requestPasswordReset");

        doExecuteTask(user, "requestPasswordReset", variables);
        userDAO.save(user);
    }

    @Override
    protected WorkflowResult<Pair<UserPatch, Boolean>> doConfirmPasswordReset(
            final User user, final String token, final String password) {

        Map<String, Object> variables = new HashMap<>(4);
        variables.put(TOKEN, token);
        variables.put(PASSWORD, password);
        variables.put(USER_TO, dataBinder.getUserTO(user, true));
        variables.put(EVENT, "confirmPasswordReset");

        Set<String> tasks = doExecuteTask(user, "confirmPasswordReset", variables);

        userDAO.save(user);

        PropagationByResource propByRes = engine.getRuntimeService().getVariable(
                user.getWorkflowId(), PROP_BY_RESOURCE, PropagationByResource.class);
        UserPatch updatedPatch = engine.getRuntimeService().getVariable(
                user.getWorkflowId(), USER_PATCH, UserPatch.class);
        Boolean propagateEnable = engine.getRuntimeService().getVariable(
                user.getWorkflowId(), PROPAGATE_ENABLE, Boolean.class);

        return new WorkflowResult<Pair<UserPatch, Boolean>>(
                new ImmutablePair<>(updatedPatch, propagateEnable), propByRes, tasks);
    }

    @Override
    protected void doDelete(final User user) {
        doExecuteTask(user, "delete", null);

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.DELETE, userDAO.findAllResourceNames(user));

        saveForFormSubmit(user, null, propByRes);

        if (engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceId(user.getWorkflowId()).active().list().isEmpty()) {

            userDAO.delete(user.getKey());

            if (!engine.getHistoryService().createHistoricProcessInstanceQuery().
                    processInstanceId(user.getWorkflowId()).list().isEmpty()) {

                engine.getHistoryService().deleteHistoricProcessInstance(user.getWorkflowId());
            }
        } else {
            updateStatus(user);
            userDAO.save(user);
        }
    }

    @Override
    public WorkflowResult<String> execute(final UserTO userTO, final String taskId) {
        User user = userDAO.authFind(userTO.getKey());

        final Map<String, Object> variables = new HashMap<>();
        variables.put(USER_TO, userTO);

        Set<String> performedTasks = doExecuteTask(user, taskId, variables);
        updateStatus(user);
        User updated = userDAO.save(user);

        return new WorkflowResult<>(updated.getKey(), null, performedTasks);
    }

    protected ProcessDefinition getProcessDefinition() {
        try {
            return engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(
                    ActivitiUserWorkflowAdapter.WF_PROCESS_ID).latestVersion().singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException("While accessing process " + ActivitiUserWorkflowAdapter.WF_PROCESS_ID, e);
        }

    }

    protected Model getModel(final ProcessDefinition procDef) {
        try {
            Model model = engine.getRepositoryService().createModelQuery().
                    deploymentId(procDef.getDeploymentId()).singleResult();
            if (model == null) {
                throw new NotFoundException("Could not find Model for deployment " + procDef.getDeploymentId());
            }
            return model;
        } catch (Exception e) {
            throw new WorkflowException("While accessing process " + ActivitiUserWorkflowAdapter.WF_PROCESS_ID, e);
        }
    }

    protected void exportProcessResource(final String resourceName, final OutputStream os) {
        ProcessDefinition procDef = getProcessDefinition();

        InputStream procDefIS = engine.getRepositoryService().getResourceAsStream(procDef.getDeploymentId(),
                resourceName);
        try {
            IOUtils.copy(procDefIS, os);
        } catch (IOException e) {
            LOG.error("While exporting workflow definition {}", procDef.getKey(), e);
        } finally {
            IOUtils.closeQuietly(procDefIS);
        }
    }

    protected void exportProcessModel(final OutputStream os) {
        Model model = getModel(getProcessDefinition());

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ObjectNode modelNode = (ObjectNode) objectMapper.readTree(model.getMetaInfo());
            modelNode.put(ModelDataJsonConstants.MODEL_ID, model.getId());
            modelNode.replace(MODEL_DATA_JSON_MODEL,
                    objectMapper.readTree(engine.getRepositoryService().getModelEditorSource(model.getId())));

            os.write(modelNode.toString().getBytes());
        } catch (IOException e) {
            LOG.error("While exporting workflow definition {}", model.getId(), e);
        }
    }

    @Override
    public void exportDefinition(final WorkflowDefinitionFormat format, final OutputStream os) {
        switch (format) {
            case JSON:
                exportProcessModel(os);
                break;

            case XML:
            default:
                exportProcessResource(WF_PROCESS_RESOURCE, os);
        }
    }

    @Override
    public void exportDiagram(final OutputStream os) {
        exportProcessResource(WF_DGRM_RESOURCE, os);
    }

    @Override
    public void importDefinition(final WorkflowDefinitionFormat format, final String definition) {
        Model model = getModel(getProcessDefinition());
        switch (format) {
            case JSON:
                JsonNode definitionNode;
                try {
                    definitionNode = new ObjectMapper().readTree(definition);
                    if (definitionNode.has(MODEL_DATA_JSON_MODEL)) {
                        definitionNode = definitionNode.get(MODEL_DATA_JSON_MODEL);
                    }
                    if (!definitionNode.has(BpmnJsonConverter.EDITOR_CHILD_SHAPES)) {
                        throw new IllegalArgumentException(
                                "Could not find JSON node " + BpmnJsonConverter.EDITOR_CHILD_SHAPES);
                    }

                    BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(definitionNode);
                    ActivitiImportUtils.fromXML(engine, new BpmnXMLConverter().convertToXML(bpmnModel));
                } catch (Exception e) {
                    throw new WorkflowException("While updating process "
                            + ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, e);
                }

                ActivitiImportUtils.fromJSON(
                        engine, definitionNode.toString().getBytes(), getProcessDefinition(), model);
                break;

            case XML:
            default:
                ActivitiImportUtils.fromXML(engine, definition.getBytes());

                ActivitiImportUtils.fromJSON(engine, getProcessDefinition(), model);
        }
    }

    protected WorkflowFormPropertyType fromActivitiFormType(final FormType activitiFormType) {
        WorkflowFormPropertyType result = WorkflowFormPropertyType.String;

        if ("string".equals(activitiFormType.getName())) {
            result = WorkflowFormPropertyType.String;
        }
        if ("long".equals(activitiFormType.getName())) {
            result = WorkflowFormPropertyType.Long;
        }
        if ("enum".equals(activitiFormType.getName())) {
            result = WorkflowFormPropertyType.Enum;
        }
        if ("date".equals(activitiFormType.getName())) {
            result = WorkflowFormPropertyType.Date;
        }
        if ("boolean".equals(activitiFormType.getName())) {
            result = WorkflowFormPropertyType.Boolean;
        }

        return result;
    }

    protected WorkflowFormTO getFormTO(final Task task) {
        return getFormTO(task, engine.getFormService().getTaskFormData(task.getId()));
    }

    protected WorkflowFormTO getFormTO(final Task task, final TaskFormData fd) {
        final WorkflowFormTO formTO =
                getFormTO(task.getProcessInstanceId(), task.getId(), fd.getFormKey(), fd.getFormProperties());

        BeanUtils.copyProperties(task, formTO);
        return formTO;
    }

    protected WorkflowFormTO getFormTO(final HistoricTaskInstance task) {
        final List<HistoricFormPropertyEntity> props = new ArrayList<>();

        for (HistoricDetail historicDetail
                : engine.getHistoryService().createHistoricDetailQuery().taskId(task.getId()).list()) {

            if (historicDetail instanceof HistoricFormPropertyEntity) {
                props.add((HistoricFormPropertyEntity) historicDetail);
            }
        }

        final WorkflowFormTO formTO = getHistoricFormTO(
                task.getProcessInstanceId(), task.getId(), task.getFormKey(), props);
        BeanUtils.copyProperties(task, formTO);

        final HistoricActivityInstance historicActivityInstance = engine.getHistoryService().
                createHistoricActivityInstanceQuery().
                executionId(task.getExecutionId()).activityType("userTask").activityName(task.getName()).singleResult();

        if (historicActivityInstance != null) {
            formTO.setCreateTime(historicActivityInstance.getStartTime());
            formTO.setDueDate(historicActivityInstance.getEndTime());
        }

        return formTO;
    }

    protected WorkflowFormTO getHistoricFormTO(
            final String processInstanceId,
            final String taskId,
            final String formKey,
            final List<HistoricFormPropertyEntity> props) {

        WorkflowFormTO formTO = new WorkflowFormTO();

        User user = userDAO.findByWorkflowId(processInstanceId);
        if (user == null) {
            throw new NotFoundException("User with workflow id " + processInstanceId);
        }
        formTO.setUsername(user.getUsername());

        formTO.setTaskId(taskId);
        formTO.setKey(formKey);

        for (HistoricFormPropertyEntity prop : props) {
            WorkflowFormPropertyTO propertyTO = new WorkflowFormPropertyTO();
            propertyTO.setId(prop.getPropertyId());
            propertyTO.setName(prop.getPropertyId());
            propertyTO.setValue(prop.getPropertyValue());
            formTO.addProperty(propertyTO);
        }

        return formTO;
    }

    @SuppressWarnings("unchecked")
    protected WorkflowFormTO getFormTO(
            final String processInstanceId,
            final String taskId,
            final String formKey,
            final List<FormProperty> properties) {

        WorkflowFormTO formTO = new WorkflowFormTO();

        User user = userDAO.findByWorkflowId(processInstanceId);
        if (user == null) {
            throw new NotFoundException("User with workflow id " + processInstanceId);
        }
        formTO.setUsername(user.getUsername());

        formTO.setTaskId(taskId);
        formTO.setKey(formKey);

        for (FormProperty fProp : properties) {
            WorkflowFormPropertyTO propertyTO = new WorkflowFormPropertyTO();
            BeanUtils.copyProperties(fProp, propertyTO, PROPERTY_IGNORE_PROPS);
            propertyTO.setType(fromActivitiFormType(fProp.getType()));

            if (propertyTO.getType() == WorkflowFormPropertyType.Date) {
                propertyTO.setDatePattern((String) fProp.getType().getInformation("datePattern"));
            }
            if (propertyTO.getType() == WorkflowFormPropertyType.Enum) {
                propertyTO.getEnumValues().putAll((Map<String, String>) fProp.getType().getInformation("values"));
            }

            formTO.addProperty(propertyTO);
        }

        return formTO;
    }

    @Transactional(readOnly = true)
    @Override
    public List<WorkflowFormTO> getForms() {
        List<WorkflowFormTO> forms = new ArrayList<>();

        String authUser = AuthContextUtils.getUsername();
        if (adminUser.equals(authUser)) {
            forms.addAll(getForms(engine.getTaskService().createTaskQuery().
                    taskVariableValueEquals(TASK_IS_FORM, Boolean.TRUE)));
        } else {
            User user = userDAO.findByUsername(authUser);
            if (user == null) {
                throw new NotFoundException("Syncope User " + authUser);
            }

            forms.addAll(getForms(engine.getTaskService().createTaskQuery().
                    taskVariableValueEquals(TASK_IS_FORM, Boolean.TRUE).
                    taskCandidateOrAssigned(user.getKey())));

            List<String> candidateGroups = new ArrayList<>();
            for (String groupName : userDAO.findAllGroupNames(user)) {
                candidateGroups.add(groupName);
            }
            if (!candidateGroups.isEmpty()) {
                forms.addAll(getForms(engine.getTaskService().createTaskQuery().
                        taskVariableValueEquals(TASK_IS_FORM, Boolean.TRUE).
                        taskCandidateGroupIn(candidateGroups)));
            }
        }

        return forms;
    }

    protected <T extends Query<?, ?>, U extends Object> List<WorkflowFormTO> getForms(final Query<T, U> query) {
        List<WorkflowFormTO> forms = new ArrayList<>();

        for (U obj : query.list()) {
            try {
                if (obj instanceof HistoricTaskInstance) {
                    forms.add(getFormTO((HistoricTaskInstance) obj));
                } else if (obj instanceof Task) {
                    forms.add(getFormTO((Task) obj));
                } else {
                    throw new ActivitiException(
                            "Failure retrieving form", new IllegalArgumentException("Invalid task type"));
                }
            } catch (ActivitiException e) {
                LOG.debug("No form found for task {}", obj, e);
            }
        }

        return forms;
    }

    @Override
    public WorkflowFormTO getForm(final String workflowId) {
        Task task;
        try {
            task = engine.getTaskService().createTaskQuery().processInstanceId(workflowId).singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException("While reading form for workflow instance " + workflowId, e);
        }

        TaskFormData formData;
        try {
            formData = engine.getFormService().getTaskFormData(task.getId());
        } catch (ActivitiException e) {
            LOG.debug("No form found for task {}", task.getId(), e);
            formData = null;
        }

        WorkflowFormTO result = null;
        if (formData != null && !formData.getFormProperties().isEmpty()) {
            result = getFormTO(task);
        }

        return result;
    }

    protected Pair<Task, TaskFormData> checkTask(final String taskId, final String authUser) {
        Task task;
        try {
            task = engine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new ActivitiException("NULL result");
            }
        } catch (ActivitiException e) {
            throw new NotFoundException("Activiti Task " + taskId, e);
        }

        TaskFormData formData;
        try {
            formData = engine.getFormService().getTaskFormData(task.getId());
        } catch (ActivitiException e) {
            throw new NotFoundException("Form for Activiti Task " + taskId, e);
        }

        if (!adminUser.equals(authUser)) {
            User user = userDAO.findByUsername(authUser);
            if (user == null) {
                throw new NotFoundException("Syncope User " + authUser);
            }
        }

        return new ImmutablePair<>(task, formData);
    }

    @Override
    public WorkflowFormTO claimForm(final String taskId) {
        String authUser = AuthContextUtils.getUsername();
        Pair<Task, TaskFormData> checked = checkTask(taskId, authUser);

        if (!adminUser.equals(authUser)) {
            List<Task> tasksForUser = engine.getTaskService().createTaskQuery().taskId(taskId).taskCandidateUser(
                    authUser).list();
            if (tasksForUser.isEmpty()) {
                throw new WorkflowException(
                        new IllegalArgumentException(authUser + " is not candidate for task " + taskId));
            }
        }

        Task task;
        try {
            engine.getTaskService().setOwner(taskId, authUser);
            task = engine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException("While reading task " + taskId, e);
        }

        return getFormTO(task, checked.getValue());
    }

    @Override
    public WorkflowResult<UserPatch> submitForm(final WorkflowFormTO form) {
        String authUser = AuthContextUtils.getUsername();
        Pair<Task, TaskFormData> checked = checkTask(form.getTaskId(), authUser);

        if (!checked.getKey().getOwner().equals(authUser)) {
            throw new WorkflowException(new IllegalArgumentException("Task " + form.getTaskId() + " assigned to "
                    + checked.getKey().getOwner() + " but submitted by " + authUser));
        }

        User user = userDAO.findByWorkflowId(checked.getKey().getProcessInstanceId());
        if (user == null) {
            throw new NotFoundException("User with workflow id " + checked.getKey().getProcessInstanceId());
        }

        Set<String> preTasks = getPerformedTasks(user);
        try {
            engine.getFormService().submitTaskFormData(form.getTaskId(), form.getPropertiesForSubmit());
            engine.getRuntimeService().setVariable(user.getWorkflowId(), FORM_SUBMITTER, authUser);
        } catch (ActivitiException e) {
            throwException(e, "While submitting form for task " + form.getTaskId());
        }

        Set<String> postTasks = getPerformedTasks(user);
        postTasks.removeAll(preTasks);
        postTasks.add(form.getTaskId());

        updateStatus(user);
        User updated = userDAO.save(user);

        // see if there is any propagation to be done
        PropagationByResource propByRes = engine.getRuntimeService().getVariable(
                user.getWorkflowId(), PROP_BY_RESOURCE, PropagationByResource.class);

        // fetch - if available - the encrypted password
        String clearPassword = null;
        String encryptedPwd = engine.getRuntimeService().getVariable(user.getWorkflowId(), ENCRYPTED_PWD, String.class);
        if (StringUtils.isNotBlank(encryptedPwd)) {
            clearPassword = decrypt(encryptedPwd);
        }

        // supports approval chains
        saveForFormSubmit(user, clearPassword, propByRes);

        UserPatch userPatch = engine.getRuntimeService().getVariable(user.getWorkflowId(), USER_PATCH, UserPatch.class);
        if (userPatch == null) {
            userPatch = new UserPatch();
            userPatch.setKey(updated.getKey());
            userPatch.setPassword(new PasswordPatch.Builder().onSyncope(true).value(clearPassword).build());

            if (propByRes != null) {
                userPatch.getPassword().getResources().addAll(propByRes.get(ResourceOperation.CREATE));
            }
        }

        return new WorkflowResult<>(userPatch, propByRes, postTasks);
    }
}
