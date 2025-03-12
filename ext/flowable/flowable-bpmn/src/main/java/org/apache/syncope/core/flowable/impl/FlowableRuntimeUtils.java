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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.attrvalue.ParsingValidationException;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.identityconnectors.common.security.EncryptorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlowableRuntimeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FlowableRuntimeUtils.class);

    public static final String WF_PROCESS_ID = "userWorkflow";

    public static final String USER = "user";

    public static final String WF_EXECUTOR = "wfExecutor";

    public static final String FORM_SUBMITTER = "formSubmitter";

    public static final String USER_CR = "userCR";

    public static final String USER_TO = "userTO";

    public static final String ENABLED = "enabled";

    public static final String USER_UR = "userUR";

    public static final String TASK = "task";

    public static final String TOKEN = "token";

    public static final String PASSWORD = "password";

    public static final String PROP_BY_RESOURCE = "propByResource";

    public static final String PROP_BY_LINKEDACCOUNT = "propByLinkedAccount";

    public static final String PROPAGATE_ENABLE = "propagateEnable";

    public static final String ENCRYPTED_PWD = "encryptedPwd";

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
        return Optional.ofNullable(procInst).map(Execution::getId).orElse(null);
    }

    public static String getProcBusinessKey(final String procDefId, final String userKey) {
        return procDefId + ':' + userKey;
    }

    public static Pair<String, String> splitProcBusinessKey(final String procBusinessKey) {
        String[] split = procBusinessKey.split(":");
        if (split.length != 2) {
            throw new WorkflowException(new IllegalArgumentException("Unexpected business key: " + procBusinessKey));
        }

        return Pair.of(split[0], split[1]);
    }

    public static ProcessDefinition getLatestProcDefByKey(final DomainProcessEngine engine, final String key) {
        try {
            return engine.getRepositoryService().createProcessDefinitionQuery().
                    processDefinitionKey(key).latestVersion().singleResult();
        } catch (FlowableException e) {
            throw new WorkflowException("While accessing process " + key, e);
        }
    }

    public static Set<String> getPerformedTasks(final DomainProcessEngine engine, final String procInstId) {
        return engine.getHistoryService().createHistoricActivityInstanceQuery().
                executionId(procInstId).
                list().stream().
                map(HistoricActivityInstance::getActivityId).
                collect(Collectors.toSet());
    }

    public static void updateStatus(final DomainProcessEngine engine, final String procInstId, final User user) {
        List<Task> tasks = engine.getTaskService().createTaskQuery().processInstanceId(procInstId).list();
        if (tasks.isEmpty() || tasks.size() > 1) {
            LOG.warn("While setting user status: unexpected task number ({})", tasks.size());
        } else {
            user.setStatus(tasks.getFirst().getTaskDefinitionKey());
        }
    }

    public static String getFormTask(final DomainProcessEngine engine, final String procInstId) {
        String result = null;

        List<Task> tasks = engine.getTaskService().createTaskQuery().
                taskWithFormKey().processInstanceId(procInstId).list();
        if (tasks.isEmpty() || tasks.size() > 1) {
            LOG.debug("While checking if form task: unexpected task number ({})", tasks.size());
        } else {
            result = tasks.getFirst().getFormKey();
        }

        return result;
    }

    /**
     * Saves resources to be propagated and password for later - after form submission - propagation.
     *
     * @param engine Flowable engine
     * @param procInstId process instance id
     * @param userTO user transfer object
     * @param password password
     * @param enabled is user to be enabled or not?
     * @param propByRes current propagation actions against resources
     * @param propByLinkedAccount current propagation actions for linked accounts
     */
    public static void saveForFormSubmit(
            final DomainProcessEngine engine,
            final String procInstId,
            final UserTO userTO,
            final String password,
            final Boolean enabled,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount) {

        String formTaskId = getFormTask(engine, procInstId);
        if (formTaskId == null) {
            return;
        }

        engine.getRuntimeService().setVariable(procInstId, FlowableRuntimeUtils.USER_TO, userTO);

        if (password == null) {
            String encryptedPwd = engine.getRuntimeService().
                    getVariable(procInstId, FlowableRuntimeUtils.ENCRYPTED_PWD, String.class);
            if (encryptedPwd != null) {
                userTO.setPassword(decrypt(encryptedPwd));
            }
        } else {
            userTO.setPassword(password);
            engine.getRuntimeService().
                    setVariable(procInstId, FlowableRuntimeUtils.ENCRYPTED_PWD, encrypt(password));
        }

        engine.getRuntimeService().setVariable(
                procInstId, FlowableRuntimeUtils.ENABLED, enabled);

        engine.getRuntimeService().setVariable(
                procInstId, FlowableRuntimeUtils.PROP_BY_RESOURCE, propByRes);
        if (propByRes != null) {
            propByRes.clear();
        }

        engine.getRuntimeService().setVariable(
                procInstId, FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, propByLinkedAccount);
        if (propByLinkedAccount != null) {
            propByLinkedAccount.clear();
        }
    }

    public static void throwException(final FlowableException e, final String defaultMessage) {
        if (e.getCause() == null) {
            throw new WorkflowException(defaultMessage, e);
        } else if (e.getCause() instanceof SyncopeClientException syncopeClientException) {
            throw syncopeClientException;
        } else if (e.getCause() instanceof ParsingValidationException parsingValidationException) {
            throw parsingValidationException;
        } else if (e.getCause() instanceof InvalidEntityException invalidEntityException) {
            throw invalidEntityException;
        } else if (e.getCause().getClass().getName().contains("persistence")) {
            throw (RuntimeException) e.getCause();
        }

        throw new WorkflowException(defaultMessage, e);
    }

    private FlowableRuntimeUtils() {
        // private constructor for static utility class
    }
}
