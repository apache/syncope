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
package org.apache.syncope.core.init;

import org.apache.syncope.core.workflow.WorkflowInstanceLoader;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class WorkflowAdapterLoader implements BeanFactoryAware {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowAdapterLoader.class);

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private RoleWorkflowAdapter rwfAdapter;

    private DefaultListableBeanFactory beanFactory;

    private WorkflowInstanceLoader wfLoader;

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    private void lazyInit() {
        if (wfLoader == null) {
            if (uwfAdapter.getLoaderClass() != null) {
                wfLoader = (WorkflowInstanceLoader) beanFactory.createBean(
                        uwfAdapter.getLoaderClass(), AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
            }
            if (rwfAdapter.getLoaderClass() != null) {
                wfLoader = (WorkflowInstanceLoader) beanFactory.createBean(
                        rwfAdapter.getLoaderClass(), AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
            }
        }
    }

    public String getTablePrefix() {
        lazyInit();
        return wfLoader == null ? null : wfLoader.getTablePrefix();
    }

    public String[] getInitSQLStatements() {
        lazyInit();
        return wfLoader == null ? null : wfLoader.getInitSQLStatements();
    }

    public void load() {
        lazyInit();
        if (wfLoader == null) {
            LOG.debug("Configured workflow adapter does not need loading");
        } else {
            LOG.debug("Loading workflow adapter by {}", wfLoader.getClass().getName());
            wfLoader.load();
        }
    }
}
