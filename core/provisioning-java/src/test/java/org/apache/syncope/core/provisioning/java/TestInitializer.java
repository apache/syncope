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
package org.apache.syncope.core.provisioning.java;

import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.content.ContentLoader;
import org.apache.syncope.core.persistence.jpa.StartupDomainLoader;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class TestInitializer implements InitializingBean, ApplicationContextAware {

    private ConfigurableApplicationContext ctx;

    @Autowired
    private StartupDomainLoader domainLoader;

    @Autowired
    private DomainHolder domainHolder;

    @Autowired
    private ContentLoader contentLoader;

    @Override
    public void setApplicationContext(final ApplicationContext ctx) throws BeansException {
        this.ctx = (ConfigurableApplicationContext) ctx;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ApplicationContextProvider.setApplicationContext(ctx);
        ApplicationContextProvider.setBeanFactory((DefaultListableBeanFactory) ctx.getBeanFactory());

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }

        domainLoader.load();

        contentLoader.load(
                SyncopeConstants.MASTER_DOMAIN,
                domainHolder.getDomains().get(SyncopeConstants.MASTER_DOMAIN));
    }
}
