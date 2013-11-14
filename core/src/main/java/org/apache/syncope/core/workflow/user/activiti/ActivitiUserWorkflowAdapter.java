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
package org.apache.syncope.core.workflow.user.activiti;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
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
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormPropertyTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.WorkflowFormPropertyType;
import org.apache.syncope.common.util.BeanUtils;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.validation.attrvalue.ParsingValidationException;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.workflow.WorkflowDefinitionFormat;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.WorkflowInstanceLoader;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.AbstractUserWorkflowAdapter;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Activiti (http://www.activiti.org/) based implementation.
 */
public class ActivitiUserWorkflowAdapter extends AbstractUserWorkflowAdapter {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ActivitiUserWorkflowAdapter.class);

    private static final String[] PROPERTY_IGNORE_PROPS = { "type" };

    public static final String WF_PROCESS_ID = "userWorkflow";

    public static final String WF_PROCESS_RESOURCE = "userWorkflow.bpmn20.xml";

    public static final String WF_DGRM_RESOURCE = "userWorkflow.userWorkflow.png";

    public static final String SYNCOPE_USER = "syncopeUser";

    public static final String WF_EXECUTOR = "wfExecutor";

    public static final String USER_TO = "userTO";

    public static final String ENABLED = "enabled";

    public static final String USER_MOD = "userMod";

    public static final String EMAIL_KIND = "emailKind";

    public static final String TASK = "task";

    public static final String TOKEN = "token";

    public static final String PROP_BY_RESOURCE = "propByResource";

    public static final String PROPAGATE_ENABLE = "propagateEnable";

    public static final String ENCRYPTED_PWD = "encryptedPwd";

    public static final String TASK_IS_FORM = "taskIsForm";

    public static final String MODEL_DATA_JSON_MODEL = "model";

    @Resource(name = "adminUser")
    private String adminUser;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FormService formService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ActivitiImportUtils importUtils;

    @Override
    public Class<? extends WorkflowInstanceLoader> getLoaderClass() {
        return ActivitiWorkflowLoader.class;
    }

    private void throwException(final ActivitiException e, final String defaultMessage) {
        if (e.getCause() != null) {
            if (e.getCause().getCause() instanceof SyncopeClientException) {
                throw (SyncopeClientException) e.getCause().getCause();
            } else if (e.getCause().getCause() instanceof ParsingValidationException) {
                throw (ParsingValidationException) e.getCause().getCause();
            }
        }

        throw new WorkflowException(defaultMessage, e);
    }

    private void updateStatus(final SyncopeUser user) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(user.getWorkflowId()).list();
        if (tasks.isEmpty() || tasks.size() > 1) {
            LOG.warn("While setting user status: unexpected task number ({})", tasks.size());
        } else {
            user.setStatus(tasks.get(0).getTaskDefinitionKey());
        }
    }

    private String getFormTask(final SyncopeUser user) {
        String result = null;

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(user.getWorkflowId()).list();
        if (tasks.isEmpty() || tasks.size() > 1) {
            LOG.warn("While checking if form task: unexpected task number ({})", tasks.size());
        } else {
            try {
                TaskFormData formData = formService.getTaskFormData(tasks.get(0).getId());
                if (formData != null && !formData.getFormProperties().isEmpty()) {
                    result = tasks.get(0).getId();
                }
            } catch (ActivitiException e) {
                LOG.warn("Could not get task form data", e);
            }
        }

        return result;
    }

    private Set<String> getPerformedTasks(final SyncopeUser user) {
        final Set<String> result = new HashSet<String>();

        for (HistoricActivityInstance task : historyService.createHistoricActivityInstanceQuery().executionId(user.
                getWorkflowId()).list()) {
            result.add(task.getActivityId());
        }

        return result;
    }

    /**
     * Saves resources to be propagated and password for later - after form submission - propagation.
     */
    private void saveForFormSubmit(final SyncopeUser user, final String password,
            final PropagationByResource propByRes) {

        String formTaskId = getFormTask(user);
        if (formTaskId != null) {
            // SYNCOPE-238: This is needed to simplify the task query in this.getForms()
            taskService.setVariableLocal(formTaskId, TASK_IS_FORM, Boolean.TRUE);
            runtimeService.setVariable(user.getWorkflowId(), PROP_BY_RESOURCE, propByRes);
            if (propByRes != null) {
                propByRes.clear();
            }

            if (StringUtils.isNotBlank(password)) {
                runtimeService.setVariable(user.getWorkflowId(), ENCRYPTED_PWD, encrypt(password));
            }
        }
    }

    @Override
    public WorkflowResult<Map.Entry<Long, Boolean>> create(final UserTO userTO, final boolean disablePwdPolicyCheck)
            throws WorkflowException {

        return create(userTO, disablePwdPolicyCheck, null);
    }

    @Override
    public WorkflowResult<Map.Entry<Long, Boolean>> create(final UserTO userTO, final boolean disablePwdPolicyCheck,
            final Boolean enabled)
            throws WorkflowException {

        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(WF_EXECUTOR, EntitlementUtil.getAuthenticatedUsername());
        variables.put(USER_TO, userTO);
        variables.put(ENABLED, enabled);

        ProcessInstance processInstance = null;
        try {
            processInstance = runtimeService.startProcessInstanceByKey(WF_PROCESS_ID, variables);
        } catch (ActivitiException e) {
            throwException(e, "While starting " + WF_PROCESS_ID + " instance");
        }

        SyncopeUser user = (SyncopeUser) runtimeService.getVariable(
                processInstance.getProcessInstanceId(), SYNCOPE_USER);

        Boolean updatedEnabled = (Boolean) runtimeService.getVariable(
                processInstance.getProcessInstanceId(), ENABLED);
        if (updatedEnabled != null) {
            user.setSuspended(!updatedEnabled);
        }

        // this will make SyncopeUserValidator not to consider password policies at all
        if (disablePwdPolicyCheck) {
            user.removeClearPassword();
        }

        updateStatus(user);
        user = userDAO.save(user);

        Boolean propagateEnable = (Boolean) runtimeService.getVariable(
                processInstance.getProcessInstanceId(), PROPAGATE_ENABLE);
        if (propagateEnable == null) {
            propagateEnable = enabled;
        }

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.CREATE, user.getResourceNames());

        saveForFormSubmit(user, userTO.getPassword(), propByRes);

        return new WorkflowResult<Map.Entry<Long, Boolean>>(
                new SimpleEntry<Long, Boolean>(user.getId(), propagateEnable), propByRes, getPerformedTasks(user));
    }

    private Set<String> doExecuteTask(final SyncopeUser user, final String task,
            final Map<String, Object> moreVariables) throws WorkflowException {

        Set<String> preTasks = getPerformedTasks(user);

        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(WF_EXECUTOR, EntitlementUtil.getAuthenticatedUsername());
        variables.put(TASK, task);

        // using BeanUtils to access all user's properties and trigger lazy loading - we are about to
        // serialize a SyncopeUser instance for availability within workflow tasks, and this breaks transactions
        BeanUtils.copyProperties(user, new SyncopeUser());
        variables.put(SYNCOPE_USER, user);

        if (moreVariables != null && !moreVariables.isEmpty()) {
            variables.putAll(moreVariables);
        }

        if (StringUtils.isBlank(user.getWorkflowId())) {
            throw new WorkflowException(new NotFoundException("Empty workflow id for " + user));
        }

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(user.getWorkflowId()).list();
        if (tasks.size() == 1) {
            try {
                taskService.complete(tasks.get(0).getId(), variables);
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
    protected WorkflowResult<Long> doActivate(final SyncopeUser user, final String token)
            throws WorkflowException {

        Set<String> tasks = doExecuteTask(user, "activate", Collections.singletonMap(TOKEN, (Object) token));

        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, tasks);
    }

    @Override
    protected WorkflowResult<Map.Entry<UserMod, Boolean>> doUpdate(final SyncopeUser user, final UserMod userMod)
            throws WorkflowException {

        Set<String> tasks = doExecuteTask(user, "update", Collections.singletonMap(USER_MOD, (Object) userMod));

        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        PropagationByResource propByRes = (PropagationByResource) runtimeService.getVariable(
                user.getWorkflowId(), PROP_BY_RESOURCE);

        saveForFormSubmit(updated, userMod.getPassword(), propByRes);

        Boolean propagateEnable = (Boolean) runtimeService.getVariable(user.getWorkflowId(), PROPAGATE_ENABLE);

        return new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                new SimpleEntry<UserMod, Boolean>(userMod, propagateEnable), propByRes, tasks);
    }

    @Override
    @Transactional(rollbackFor = { Throwable.class })
    protected WorkflowResult<Long> doSuspend(final SyncopeUser user)
            throws WorkflowException {

        Set<String> performedTasks = doExecuteTask(user, "suspend", null);
        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, performedTasks);
    }

    @Override
    protected WorkflowResult<Long> doReactivate(final SyncopeUser user)
            throws WorkflowException {

        Set<String> performedTasks = doExecuteTask(user, "reactivate", null);
        updateStatus(user);

        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, performedTasks);
    }

    @Override
    protected void doDelete(final SyncopeUser user)
            throws WorkflowException {

        doExecuteTask(user, "delete", null);

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.DELETE, user.getResourceNames());

        saveForFormSubmit(user, null, propByRes);

        if (runtimeService.createProcessInstanceQuery().
                processInstanceId(user.getWorkflowId()).active().list().isEmpty()) {

            userDAO.delete(user.getId());

            if (!historyService.createHistoricProcessInstanceQuery().
                    processInstanceId(user.getWorkflowId()).list().isEmpty()) {

                historyService.deleteHistoricProcessInstance(user.getWorkflowId());
            }
        } else {
            updateStatus(user);
            userDAO.save(user);
        }
    }

    @Override
    public WorkflowResult<Long> execute(final UserTO userTO, final String taskId)
            throws UnauthorizedRoleException, WorkflowException {

        SyncopeUser user = dataBinder.getUserFromId(userTO.getId());

        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(USER_TO, userTO);

        Set<String> performedTasks = doExecuteTask(user, taskId, variables);
        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, performedTasks);
    }

    protected ProcessDefinition getProcessDefinition() {
        try {
            return repositoryService.createProcessDefinitionQuery().processDefinitionKey(
                    ActivitiUserWorkflowAdapter.WF_PROCESS_ID).latestVersion().singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException("While accessing process " + ActivitiUserWorkflowAdapter.WF_PROCESS_ID, e);
        }

    }

    protected Model getModel(final ProcessDefinition procDef) {
        try {
            Model model = repositoryService.createModelQuery().deploymentId(procDef.getDeploymentId()).singleResult();
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

        InputStream procDefIS = repositoryService.getResourceAsStream(procDef.getDeploymentId(), resourceName);
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
            modelNode.put(MODEL_DATA_JSON_MODEL,
                    objectMapper.readTree(repositoryService.getModelEditorSource(model.getId())));

            os.write(modelNode.toString().getBytes());
        } catch (IOException e) {
            LOG.error("While exporting workflow definition {}", model.getKey(), e);
        }
    }

    @Override
    public void exportDefinition(final WorkflowDefinitionFormat format, final OutputStream os)
            throws WorkflowException {

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
    public void exportDiagram(final OutputStream os) throws WorkflowException {
        exportProcessResource(WF_DGRM_RESOURCE, os);
    }

    @Override
    public void importDefinition(final WorkflowDefinitionFormat format, final String definition)
            throws WorkflowException {

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
                    importUtils.fromXML(new BpmnXMLConverter().convertToXML(bpmnModel));
                } catch (Exception e) {
                    throw new WorkflowException("While updating process "
                            + ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, e);
                }

                importUtils.fromJSON(definitionNode.toString().getBytes(), getProcessDefinition(), model);
                break;

            case XML:
            default:
                importUtils.fromXML(definition.getBytes());

                importUtils.fromJSON(getProcessDefinition(), model);
        }
    }

    @Override
    public List<String> getDefinedTasks()
            throws WorkflowException {

        List<String> result = new ArrayList<String>();

        ProcessDefinition procDef = getProcessDefinition();

        InputStream procDefIS = repositoryService.
                getResourceAsStream(procDef.getDeploymentId(), WF_PROCESS_RESOURCE);

        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(procDefIS);

            XPath xpath = XPathFactory.newInstance().newXPath();

            NodeList nodeList = (NodeList) xpath.evaluate("//userTask | //serviceTask | //scriptTask", doc,
                    XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                result.add(nodeList.item(i).getAttributes().getNamedItem("id").getNodeValue());
            }
        } catch (Exception e) {
            throw new WorkflowException("While reading defined tasks", e);
        } finally {
            IOUtils.closeQuietly(procDefIS);
        }

        return result;
    }

    private WorkflowFormPropertyType fromActivitiFormType(final FormType activitiFormType) {
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

    private WorkflowFormTO getFormTO(final Task task) {
        return getFormTO(task, formService.getTaskFormData(task.getId()));
    }

    private WorkflowFormTO getFormTO(final Task task, final TaskFormData fd) {
        final WorkflowFormTO formTO =
                getFormTO(task.getProcessInstanceId(), task.getId(), fd.getFormKey(), fd.getFormProperties());

        BeanUtils.copyProperties(task, formTO);
        return formTO;
    }

    private WorkflowFormTO getFormTO(final HistoricTaskInstance task) {
        final List<HistoricFormPropertyEntity> props = new ArrayList<HistoricFormPropertyEntity>();

        for (HistoricDetail historicDetail : historyService.createHistoricDetailQuery().taskId(task.getId()).list()) {

            if (historicDetail instanceof HistoricFormPropertyEntity) {
                props.add((HistoricFormPropertyEntity) historicDetail);
            }
        }

        final WorkflowFormTO formTO = getHistoricFormTO(
                task.getProcessInstanceId(), task.getId(), task.getFormKey(), props);
        BeanUtils.copyProperties(task, formTO);

        final HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().
                executionId(task.getExecutionId()).activityType("userTask").activityName(task.getName()).singleResult();

        if (historicActivityInstance != null) {
            formTO.setCreateTime(historicActivityInstance.getStartTime());
            formTO.setDueDate(historicActivityInstance.getEndTime());
        }

        return formTO;
    }

    private WorkflowFormTO getHistoricFormTO(
            final String processInstanceId,
            final String taskId,
            final String formKey,
            final List<HistoricFormPropertyEntity> props) {

        WorkflowFormTO formTO = new WorkflowFormTO();

        SyncopeUser user = userDAO.findByWorkflowId(processInstanceId);
        if (user == null) {
            throw new NotFoundException("User with workflow id " + processInstanceId);
        }
        formTO.setUserId(user.getId());

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
    private WorkflowFormTO getFormTO(
            final String processInstanceId,
            final String taskId,
            final String formKey,
            final List<FormProperty> properties) {

        WorkflowFormTO formTO = new WorkflowFormTO();

        SyncopeUser user = userDAO.findByWorkflowId(processInstanceId);
        if (user == null) {
            throw new NotFoundException("User with workflow id " + processInstanceId);
        }
        formTO.setUserId(user.getId());

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

    @Override
    public List<WorkflowFormTO> getForms() {
        return getForms(taskService.createTaskQuery().taskVariableValueEquals(TASK_IS_FORM, Boolean.TRUE));
    }

    @Override
    public List<WorkflowFormTO> getForms(final String workflowId, final String name) {
        final List<WorkflowFormTO> forms = getForms(
                taskService.createTaskQuery().processInstanceId(workflowId).taskName(name).
                taskVariableValueEquals(TASK_IS_FORM, Boolean.TRUE));

        forms.addAll(getForms(historyService.createHistoricTaskInstanceQuery().taskName(name)
                .taskVariableValueEquals(TASK_IS_FORM, Boolean.TRUE)));

        return forms;
    }

    private <T extends Query<?, ?>, U extends Object> List<WorkflowFormTO> getForms(final Query<T, U> query) {
        final List<WorkflowFormTO> forms = new ArrayList<WorkflowFormTO>();

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
    public WorkflowFormTO getForm(final String workflowId)
            throws NotFoundException, WorkflowException {

        Task task;
        try {
            task = taskService.createTaskQuery().processInstanceId(workflowId).singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException("While reading form for workflow instance " + workflowId, e);
        }

        TaskFormData formData;
        try {
            formData = formService.getTaskFormData(task.getId());
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

    private Map.Entry<Task, TaskFormData> checkTask(final String taskId, final String username) {
        Task task;
        try {
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
        } catch (ActivitiException e) {
            throw new NotFoundException("Activiti Task " + taskId, e);
        }

        TaskFormData formData;
        try {
            formData = formService.getTaskFormData(task.getId());
        } catch (ActivitiException e) {
            throw new NotFoundException("Form for Activiti Task " + taskId, e);
        }

        if (!adminUser.equals(username)) {
            SyncopeUser user = userDAO.find(username);
            if (user == null) {
                throw new NotFoundException("Syncope User " + username);
            }
        }

        return new SimpleEntry<Task, TaskFormData>(task, formData);
    }

    @Override
    public WorkflowFormTO claimForm(final String taskId, final String username)
            throws WorkflowException {

        Map.Entry<Task, TaskFormData> checked = checkTask(taskId, username);

        if (!adminUser.equals(username)) {
            List<Task> tasksForUser = taskService.createTaskQuery().taskId(taskId).taskCandidateUser(username).list();
            if (tasksForUser.isEmpty()) {
                throw new WorkflowException(
                        new IllegalArgumentException(username + " is not candidate for task " + taskId));
            }
        }

        Task task;
        try {
            taskService.setOwner(taskId, username);
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException("While reading task " + taskId, e);
        }

        return getFormTO(task, checked.getValue());
    }

    @Override
    public WorkflowResult<UserMod> submitForm(final WorkflowFormTO form, final String username)
            throws WorkflowException {

        Map.Entry<Task, TaskFormData> checked = checkTask(form.getTaskId(), username);

        if (!checked.getKey().getOwner().equals(username)) {
            throw new WorkflowException(new IllegalArgumentException("Task " + form.getTaskId() + " assigned to "
                    + checked.getKey().getOwner() + " but submited by " + username));
        }

        SyncopeUser user = userDAO.findByWorkflowId(checked.getKey().getProcessInstanceId());
        if (user == null) {
            throw new NotFoundException("User with workflow id " + checked.getKey().getProcessInstanceId());
        }

        Set<String> preTasks = getPerformedTasks(user);
        try {
            formService.submitTaskFormData(form.getTaskId(), form.getPropertiesForSubmit());
        } catch (ActivitiException e) {
            throwException(e, "While submitting form for task " + form.getTaskId());
        }

        Set<String> postTasks = getPerformedTasks(user);
        postTasks.removeAll(preTasks);
        postTasks.add(form.getTaskId());

        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        // see if there is any propagation to be done
        PropagationByResource propByRes =
                (PropagationByResource) runtimeService.getVariable(user.getWorkflowId(), PROP_BY_RESOURCE);

        // fetch - if available - the encrypted password
        String clearPassword = null;
        String encryptedPwd = (String) runtimeService.getVariable(user.getWorkflowId(), ENCRYPTED_PWD);
        if (StringUtils.isNotBlank(encryptedPwd)) {
            clearPassword = decrypt(encryptedPwd);
        }

        UserMod userMod = (UserMod) runtimeService.getVariable(user.getWorkflowId(), USER_MOD);
        if (userMod == null) {
            userMod = new UserMod();
            userMod.setId(updated.getId());
            userMod.setPassword(clearPassword);
        }

        return new WorkflowResult<UserMod>(userMod, propByRes, postTasks);
    }
}
