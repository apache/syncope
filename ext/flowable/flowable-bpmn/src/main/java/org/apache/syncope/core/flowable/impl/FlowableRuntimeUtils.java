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

import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.identityconnectors.common.security.EncryptorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlowableRuntimeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FlowableRuntimeUtils.class);

    public static final String WF_PROCESS_ID = "userWorkflow";

    public static final String USER = "user";

    public static final String WF_EXECUTOR = "wfExecutor";

    public static final String FORM_SUBMITTER = "formSubmitter";

    public static final String USER_TO = "userTO";

    public static final String ENABLED = "enabled";

    public static final String USER_PATCH = "userPatch";

    public static final String TASK = "task";

    public static final String TOKEN = "token";

    public static final String PASSWORD = "password";

    public static final String PROP_BY_RESOURCE = "propByResource";

    public static final String PROPAGATE_ENABLE = "propagateEnable";

    public static final String ENCRYPTED_PWD = "encryptedPwd";

    public static final String STORE_PASSWORD = "storePassword";

    public static final String EVENT = "event";

    public static String encrypt(final String clear) {
        byte[] encrypted = EncryptorFactory.getInstance().getDefaultEncryptor().encrypt(clear.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(final String crypted) {
        byte[] decrypted = EncryptorFactory.getInstance().getDefaultEncryptor().
                decrypt(Base64.getDecoder().decode(crypted));
        return new String(decrypted);
    }

    public static String getWFProcBusinessKey(final String userKey) {
        return FlowableRuntimeUtils.getProcBusinessKey(WF_PROCESS_ID, userKey);
    }

    public static String getWFProcInstID(final DomainProcessEngine engine, final String userKey) {
        ProcessInstance procInst = engine.getRuntimeService().createProcessInstanceQuery().
                processInstanceBusinessKey(getWFProcBusinessKey(userKey)).singleResult();
        return procInst == null ? null : procInst.getId();
    }

    public static String getProcBusinessKey(final String processDefinitionId, final String userKey) {
        return processDefinitionId + ":" + userKey;
    }

    public static ProcessDefinition getLatestProcDefByKey(final DomainProcessEngine engine, final String key) {
        try {
            return engine.getRepositoryService().createProcessDefinitionQuery().
                    processDefinitionKey(key).latestVersion().singleResult();
        } catch (FlowableException e) {
            throw new WorkflowException("While accessing process " + key, e);
        }
    }

    public static Set<String> getPerformedTasks(
            final DomainProcessEngine engine, final String procInstID, final User user) {

        return engine.getHistoryService().createHistoricActivityInstanceQuery().
                executionId(procInstID).
                list().stream().
                map(HistoricActivityInstance::getActivityId).
                collect(Collectors.toSet());
    }

    public static void updateStatus(final DomainProcessEngine engine, final String procInstID, final User user) {
        List<Task> tasks = createTaskQuery(engine, false).processInstanceId(procInstID).list();
        if (tasks.isEmpty() || tasks.size() > 1) {
            LOG.warn("While setting user status: unexpected task number ({})", tasks.size());
        } else {
            user.setStatus(tasks.get(0).getTaskDefinitionKey());
        }
    }

    public static List<ProcessInstance> getProcessInstances(final DomainProcessEngine engine, final String userKey) {
        return engine.getRuntimeService().createNativeProcessInstanceQuery().
                sql("SELECT ID_,PROC_INST_ID_ FROM " + engine.getManagementService().getTableName(ExecutionEntity.class)
                        + " WHERE BUSINESS_KEY_ LIKE '" + getProcBusinessKey("%", userKey) + "'"
                        + " AND PARENT_ID_ IS NULL").list();
    }

    public static TaskQuery createTaskQuery(final DomainProcessEngine engine, final boolean onlyFormTasks) {
        SyncopeTaskQueryImpl taskQuery = new SyncopeTaskQueryImpl(
                ((RuntimeServiceImpl) engine.getRuntimeService()).getCommandExecutor());
        if (onlyFormTasks) {
            taskQuery.taskWithFormKey();
        }
        return taskQuery;
    }

    public static String getFormTask(final DomainProcessEngine engine, final String procInstID) {
        String result = null;

        List<Task> tasks = createTaskQuery(engine, true).processInstanceId(procInstID).list();
        if (tasks.isEmpty() || tasks.size() > 1) {
            LOG.debug("While checking if form task: unexpected task number ({})", tasks.size());
        } else {
            result = tasks.get(0).getFormKey();
        }

        return result;
    }

    /**
     * Saves resources to be propagated and password for later - after form submission - propagation.
     *
     * @param engine Flowable engine
     * @param procInstID process instance id
     * @param user user JPA entity
     * @param userTO user transfer object
     * @param password password
     * @param enabled is user to be enabled or not?
     * @param propByRes current propagation actions against resources
     */
    public static void saveForFormSubmit(
            final DomainProcessEngine engine,
            final String procInstID,
            final User user,
            final UserTO userTO,
            final String password,
            final Boolean enabled,
            final PropagationByResource propByRes) {

        String formTaskId = getFormTask(engine, procInstID);
        if (formTaskId == null) {
            return;
        }

        engine.getRuntimeService().setVariable(procInstID, FlowableRuntimeUtils.USER_TO, userTO);

        if (password == null) {
            String encryptedPwd = engine.getRuntimeService().
                    getVariable(procInstID, FlowableRuntimeUtils.ENCRYPTED_PWD, String.class);
            if (encryptedPwd != null) {
                userTO.setPassword(decrypt(encryptedPwd));
            }
        } else {
            userTO.setPassword(password);
            engine.getRuntimeService().
                    setVariable(procInstID, FlowableRuntimeUtils.ENCRYPTED_PWD, encrypt(password));
        }

        engine.getRuntimeService().setVariable(procInstID, FlowableRuntimeUtils.ENABLED, enabled);

        engine.getRuntimeService().setVariable(procInstID, FlowableRuntimeUtils.PROP_BY_RESOURCE, propByRes);
        if (propByRes != null) {
            propByRes.clear();
        }
    }

    public static void throwException(final FlowableException e, final String defaultMessage) {
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

    private FlowableRuntimeUtils() {
        // private constructor for static utility class
    }
}
