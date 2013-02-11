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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
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
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowDefinitionTO;
import org.apache.syncope.common.to.WorkflowFormPropertyTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.WorkflowFormPropertyType;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.WorkflowInstanceLoader;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.AbstractUserWorkflowAdapter;
import org.identityconnectors.common.security.EncryptorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
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

    private static final String[] PROPERTY_IGNORE_PROPS = {"type"};

    public static final String WF_PROCESS_ID = "userWorkflow";

    public static final String WF_PROCESS_RESOURCE = "userWorkflow.bpmn20.xml";

    public static final String SYNCOPE_USER = "syncopeUser";

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

    @Override
    public Class<? extends WorkflowInstanceLoader> getLoaderClass() {
        return ActivitiWorkflowLoader.class;
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
        Set<String> result = new HashSet<String>();

        List<HistoricActivityInstance> tasks = historyService.createHistoricActivityInstanceQuery().executionId(
                user.getWorkflowId()).list();
        for (HistoricActivityInstance task : tasks) {
            result.add(task.getActivityId());
        }

        return result;
    }

    private String encrypt(final String clear) {
        byte[] encryptedBytes = EncryptorFactory.getInstance().getDefaultEncryptor().encrypt(clear.getBytes());

        return new String(Base64.encode(encryptedBytes));
    }

    private String decrypt(final String crypted) {
        byte[] decryptedBytes =
                EncryptorFactory.getInstance().getDefaultEncryptor().decrypt(Base64.decode(crypted.getBytes()));

        return new String(decryptedBytes);
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
        variables.put(USER_TO, userTO);
        variables.put(ENABLED, enabled);

        final ProcessInstance processInstance;
        try {
            processInstance = runtimeService.startProcessInstanceByKey("userWorkflow", variables);
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }

        SyncopeUser user = (SyncopeUser) runtimeService.getVariable(processInstance.getProcessInstanceId(),
                SYNCOPE_USER);

        // this will make SyncopeUserValidator not to consider password policies at all
        if (disablePwdPolicyCheck) {
            user.removeClearPassword();
        }

        updateStatus(user);
        user = userDAO.save(user);

        Boolean propagateEnable = (Boolean) runtimeService.getVariable(processInstance.getProcessInstanceId(),
                PROPAGATE_ENABLE);
        if (propagateEnable == null) {
            propagateEnable = enabled;
        }

        // save resources to be propagated and password for later - after form submission - propagation
        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.CREATE, user.getResourceNames());

        String formTaskId = getFormTask(user);
        if (formTaskId != null) {
            // SYNCOPE-238: This is needed to simplify the task query in this.getForms()
            taskService.setVariableLocal(formTaskId, TASK_IS_FORM, Boolean.TRUE);
            runtimeService.setVariable(processInstance.getProcessInstanceId(), PROP_BY_RESOURCE, propByRes);
            propByRes = null;

            if (StringUtils.isNotBlank(userTO.getPassword())) {
                runtimeService.setVariable(
                        processInstance.getProcessInstanceId(), ENCRYPTED_PWD, encrypt(userTO.getPassword()));
            }
        }

        return new WorkflowResult<Map.Entry<Long, Boolean>>(
                new SimpleEntry<Long, Boolean>(user.getId(), propagateEnable), propByRes, getPerformedTasks(user));
    }

    private Set<String> doExecuteTask(final SyncopeUser user, final String task,
            final Map<String, Object> moreVariables) throws WorkflowException {

        Set<String> preTasks = getPerformedTasks(user);

        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(SYNCOPE_USER, user);
        variables.put(TASK, task);

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
                throw new WorkflowException(e);
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

        Set<String> performedTasks = doExecuteTask(user, "activate", Collections.singletonMap(TOKEN, (Object) token));
        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, performedTasks);
    }

    @Override
    protected WorkflowResult<Map.Entry<Long, Boolean>> doUpdate(final SyncopeUser user, final UserMod userMod)
            throws WorkflowException {

        Set<String> task = doExecuteTask(user, "update", Collections.singletonMap(USER_MOD, (Object) userMod));

        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        PropagationByResource propByRes = (PropagationByResource) runtimeService.getVariable(user.getWorkflowId(),
                PROP_BY_RESOURCE);

        // save resources to be propagated and password for later - after form submission - propagation
        String formTaskId = getFormTask(user);
        if (formTaskId != null) {
            // SYNCOPE-238: This is needed to simplify the task query in this.getForms()
            taskService.setVariableLocal(formTaskId, TASK_IS_FORM, Boolean.TRUE);
            if (StringUtils.isNotBlank(userMod.getPassword())) {
                runtimeService.setVariable(user.getWorkflowId(), ENCRYPTED_PWD, encrypt(userMod.getPassword()));
            }
        }

        Boolean propagateEnable = (Boolean) runtimeService.getVariable(user.getWorkflowId(), PROPAGATE_ENABLE);

        return new WorkflowResult<Map.Entry<Long, Boolean>>(new SimpleEntry<Long, Boolean>(
                updated.getId(), propagateEnable), propByRes, task);
    }

    @Override
    @Transactional(rollbackFor = {Throwable.class})
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
        userDAO.delete(user);
    }

    @Override
    public WorkflowResult<Long> execute(final UserTO userTO, final String taskId)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException {

        SyncopeUser user = dataBinder.getUserFromId(userTO.getId());

        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(USER_TO, userTO);

        Set<String> performedTasks = doExecuteTask(user, taskId, variables);
        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, performedTasks);
    }

    @Override
    public WorkflowDefinitionTO getDefinition()
            throws WorkflowException {

        ProcessDefinition procDef;
        try {
            procDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(
                    ActivitiUserWorkflowAdapter.WF_PROCESS_ID).latestVersion().singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }

        InputStream procDefIS = repositoryService.getResourceAsStream(procDef.getDeploymentId(), WF_PROCESS_RESOURCE);
        Reader reader = null;
        Writer writer = new StringWriter();
        try {
            reader = new BufferedReader(new InputStreamReader(procDefIS));

            int n;
            char[] buffer = new char[1024];
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (IOException e) {
            LOG.error("While reading workflow definition {}", procDef.getKey(), e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (procDefIS != null) {
                    procDefIS.close();
                }
            } catch (IOException ioe) {
                LOG.error("While closing input stream for {}", procDef.getKey(), ioe);
            }
        }

        WorkflowDefinitionTO definitionTO = new WorkflowDefinitionTO();
        definitionTO.setId(ActivitiUserWorkflowAdapter.WF_PROCESS_ID);
        definitionTO.setXmlDefinition(writer.toString());

        return definitionTO;
    }

    @Override
    public void updateDefinition(final WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException {

        if (!ActivitiUserWorkflowAdapter.WF_PROCESS_ID.equals(definition.getId())) {

            throw new NotFoundException("Workflow process id " + definition.getId());
        }

        try {
            repositoryService.createDeployment().addInputStream(ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE,
                    new ByteArrayInputStream(definition.getXmlDefinition().getBytes())).deploy();
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }
    }

    @Override
    public List<String> getDefinedTasks()
            throws WorkflowException {

        List<String> result = new ArrayList<String>();

        ProcessDefinition procDef;
        try {
            procDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(
                    ActivitiUserWorkflowAdapter.WF_PROCESS_ID).latestVersion().singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }

        InputStream procDefIS = repositoryService.getResourceAsStream(procDef.getDeploymentId(), WF_PROCESS_RESOURCE);

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
            throw new WorkflowException(e);
        } finally {
            try {
                procDefIS.close();
            } catch (IOException ioe) {
                LOG.error("While closing input stream for {}", procDef.getKey(), ioe);
            }
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

    @SuppressWarnings("unchecked")
    private WorkflowFormTO getFormTO(final Task task, final TaskFormData formData) {
        WorkflowFormTO formTO = new WorkflowFormTO();

        SyncopeUser user = userDAO.findByWorkflowId(task.getProcessInstanceId());
        if (user == null) {
            throw new NotFoundException("User with workflow id " + task.getProcessInstanceId());
        }
        formTO.setUserId(user.getId());

        formTO.setTaskId(task.getId());
        formTO.setKey(formData.getFormKey());

        BeanUtils.copyProperties(task, formTO);

        for (FormProperty fProp : formData.getFormProperties()) {
            WorkflowFormPropertyTO propertyTO = new WorkflowFormPropertyTO();
            BeanUtils.copyProperties(fProp, propertyTO, PROPERTY_IGNORE_PROPS);
            propertyTO.setType(fromActivitiFormType(fProp.getType()));

            if (propertyTO.getType() == WorkflowFormPropertyType.Date) {
                propertyTO.setDatePattern((String) fProp.getType().getInformation("datePattern"));
            }
            if (propertyTO.getType() == WorkflowFormPropertyType.Enum) {
                propertyTO.setEnumValues((Map<String, String>) fProp.getType().getInformation("values"));
            }

            formTO.addProperty(propertyTO);
        }

        return formTO;
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        List<WorkflowFormTO> forms = new ArrayList<WorkflowFormTO>();

        for (Task task : taskService.createTaskQuery().taskVariableValueEquals(TASK_IS_FORM, Boolean.TRUE).list()) {
            try {
                TaskFormData formData = formService.getTaskFormData(task.getId());
                if (formData != null && !formData.getFormProperties().isEmpty()) {
                    forms.add(getFormTO(task, formData));
                }
            } catch (ActivitiException e) {
                LOG.debug("No form found for task {}", task.getId(), e);
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
            throw new WorkflowException(e);
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
            result = getFormTO(task, formData);
        }

        return result;
    }

    private Map.Entry<Task, TaskFormData> checkTask(final String taskId, final String username)
            throws NotFoundException {

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
            throws NotFoundException, WorkflowException {

        Map.Entry<Task, TaskFormData> checked = checkTask(taskId, username);

        if (!adminUser.equals(username)) {
            List<Task> tasksForUser = taskService.createTaskQuery().taskId(taskId).taskCandidateUser(username).list();
            if (tasksForUser.isEmpty()) {
                throw new WorkflowException(new RuntimeException(username + " is not candidate for task " + taskId));
            }
        }

        Task task;
        try {
            taskService.setOwner(taskId, username);
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }

        return getFormTO(task, checked.getValue());
    }

    @Override
    public WorkflowResult<Map.Entry<Long, String>> submitForm(final WorkflowFormTO form, final String username)
            throws NotFoundException, WorkflowException {

        Map.Entry<Task, TaskFormData> checked = checkTask(form.getTaskId(), username);

        if (!checked.getKey().getOwner().equals(username)) {
            throw new WorkflowException(new RuntimeException("Task " + form.getTaskId() + " assigned to "
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
            throw new WorkflowException(e);
        }

        Set<String> postTasks = getPerformedTasks(user);
        postTasks.removeAll(preTasks);
        postTasks.add(form.getTaskId());

        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        // see if there is any propagation to be done
        PropagationByResource propByRes = (PropagationByResource) runtimeService.getVariable(user.getWorkflowId(),
                PROP_BY_RESOURCE);

        // fetch - if available - the encrpted password
        String clearPassword = null;
        String encryptedPwd = (String) runtimeService.getVariable(user.getWorkflowId(), ENCRYPTED_PWD);
        if (StringUtils.isNotBlank(encryptedPwd)) {
            clearPassword = decrypt(encryptedPwd);
        }

        return new WorkflowResult<Map.Entry<Long, String>>(new SimpleEntry<Long, String>(updated.getId(),
                clearPassword), propByRes, postTasks);
    }
}
