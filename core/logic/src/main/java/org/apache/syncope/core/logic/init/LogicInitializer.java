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
package org.apache.syncope.core.logic.init;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * Take care of all initializations needed by Syncope logic to run up and safe.
 */
@Component
public class LogicInitializer implements InitializingBean, BeanFactoryAware {

    private static final Logger LOG = LoggerFactory.getLogger(LogicInitializer.class);

    private DefaultListableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, SyncopeLoader> loaderMap = beanFactory.getBeansOfType(SyncopeLoader.class);

        List<SyncopeLoader> loaders = new ArrayList<>(loaderMap.values());
        Collections.sort(loaders, new Comparator<SyncopeLoader>() {

            @Override
            public int compare(final SyncopeLoader o1, final SyncopeLoader o2) {
                return o1.getPriority().compareTo(o2.getPriority());
            }
        });

        ApplicationContextProvider.setBeanFactory(beanFactory);

        LOG.debug("Starting initialization...");
        for (SyncopeLoader loader : loaders) {
            LOG.debug("Invoking {} with priority {}", AopUtils.getTargetClass(loader).getName(), loader.getPriority());
            loader.load();
        }
        LOG.debug("Initialization completed");
    }
}
