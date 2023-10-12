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
package org.apache.syncope.core.starter;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;

public class TaskExecutorUnloader implements SyncopeCoreLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(TaskExecutorUnloader.class);

    protected final ListableBeanFactory beanFactory;

    protected final Map<String, ExecutorConfigurationSupport> tptes = new HashMap<>();

    public TaskExecutorUnloader(final ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void load() {
        tptes.putAll(beanFactory.getBeansOfType(ExecutorConfigurationSupport.class));
    }

    @Override
    public void unload() {
        tptes.forEach((name, tpte) -> {
            LOG.info("Shutting down {}...", name);
            try {
                tpte.shutdown();

                LOG.info("Successfully shut down {}", name);
            } catch (Exception e) {
                LOG.error("While shutting down {}", name, e);
            }
        });
    }
}
