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
package org.apache.syncope.core.workflow.flowable.spring;

import java.util.Collections;
import java.util.Map;
import javax.sql.DataSource;
import org.activiti.engine.DynamicBpmnService;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.ProcessEngineImpl;
import org.apache.syncope.core.spring.security.AuthContextUtils;

/**
 * {@link ProcessEngine} delegating actual method invocation to the inner map of {@link ProcessEngine} instances,
 * one for each Syncope domain.
 */
public class DomainProcessEngine implements ProcessEngine {

    private final Map<String, ProcessEngine> engines;

    public DomainProcessEngine(final Map<String, ProcessEngine> engines) {
        this.engines = Collections.synchronizedMap(engines);
    }

    public Map<String, ProcessEngine> getEngines() {
        return engines;
    }

    @Override
    public String getName() {
        return engines.get(AuthContextUtils.getDomain()).getName();
    }

    @Override
    public void close() {
        for (ProcessEngine engine : engines.values()) {
            engine.close();
        }
    }

    @Override
    public RepositoryService getRepositoryService() {
        return engines.get(AuthContextUtils.getDomain()).getRepositoryService();
    }

    @Override
    public RuntimeService getRuntimeService() {
        return engines.get(AuthContextUtils.getDomain()).getRuntimeService();
    }

    @Override
    public FormService getFormService() {
        return engines.get(AuthContextUtils.getDomain()).getFormService();
    }

    @Override
    public TaskService getTaskService() {
        return engines.get(AuthContextUtils.getDomain()).getTaskService();
    }

    @Override
    public HistoryService getHistoryService() {
        return engines.get(AuthContextUtils.getDomain()).getHistoryService();
    }

    @Override
    public IdentityService getIdentityService() {
        return engines.get(AuthContextUtils.getDomain()).getIdentityService();
    }

    @Override
    public ManagementService getManagementService() {
        return engines.get(AuthContextUtils.getDomain()).getManagementService();
    }

    @Override
    public ProcessEngineConfiguration getProcessEngineConfiguration() {
        return engines.get(AuthContextUtils.getDomain()).getProcessEngineConfiguration();
    }

    @Override
    public DynamicBpmnService getDynamicBpmnService() {
        return engines.get(AuthContextUtils.getDomain()).getDynamicBpmnService();
    }

    public DataSource getDataSource() {
        ProcessEngineImpl engine = (ProcessEngineImpl) engines.get(AuthContextUtils.getDomain());
        return engine.getProcessEngineConfiguration().getDataSource();
    }
}
