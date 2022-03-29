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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * Take care of all initializations needed by Syncope logic to run up and safe
 * and all clean up needed to shut down gracefully.
 */
@Component
public class LogicStartStop implements BeanFactoryAware, InitializingBean, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(LogicStartStop.class);

    private DefaultListableBeanFactory beanFactory;

    private final List<SyncopeLoader> loaders = new ArrayList<>();

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    private void execute(final Consumer<SyncopeLoader> action) {
        loaders.forEach(loader -> {
            LOG.debug("Invoking {} with priority {}", AopUtils.getTargetClass(loader).getName(), loader.getPriority());
            action.accept(loader);
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ApplicationContextProvider.setBeanFactory(beanFactory);

        loaders.addAll(beanFactory.getBeansOfType(SyncopeLoader.class).values().stream().
                sorted(Comparator.comparing(SyncopeLoader::getPriority)).collect(Collectors.toList()));

        LOG.debug("Starting init...");
        execute(SyncopeLoader::load);
        LOG.debug("Init completed");
    }

    @Override
    public void destroy() throws Exception {
        Collections.reverse(loaders);

        LOG.debug("Starting dispose...");
        execute(SyncopeLoader::unload);
        LOG.debug("Dispose completed");
    }
}
