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
package org.apache.syncope.client.console.init;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * Take care of all initializations needed by Syncope Console to run up and safe.
 */
@Component
public class ConsoleInitializer implements InitializingBean, BeanFactoryAware {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleInitializer.class);

    private DefaultListableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, SyncopeConsoleLoader> loaderMap = beanFactory.getBeansOfType(SyncopeConsoleLoader.class);

        List<SyncopeConsoleLoader> loaders = new ArrayList<>(loaderMap.values());
        Collections.sort(loaders, new Comparator<SyncopeConsoleLoader>() {

            @Override
            public int compare(final SyncopeConsoleLoader o1, final SyncopeConsoleLoader o2) {
                return o1.getPriority().compareTo(o2.getPriority());
            }
        });

        LOG.debug("Starting initialization...");
        for (SyncopeConsoleLoader loader : loaders) {
            LOG.debug("Invoking {} with priority {}", AopUtils.getTargetClass(loader).getName(), loader.getPriority());
            loader.load();
        }
        LOG.debug("Initialization completed");
    }

}
