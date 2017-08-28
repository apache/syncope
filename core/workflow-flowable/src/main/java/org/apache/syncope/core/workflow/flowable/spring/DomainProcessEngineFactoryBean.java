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

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.activiti.spring.SpringExpressionManager;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring factory for {@link DomainProcessEngine} which takes the provided {@link SpringProcessEngineConfiguration} as
 * template for each of the configured Syncope domains.
 */
public class DomainProcessEngineFactoryBean
        implements FactoryBean<DomainProcessEngine>, DisposableBean, ApplicationContextAware {

    private ApplicationContext ctx;

    private DomainProcessEngine engine;

    @Override
    public void setApplicationContext(final ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    @Override
    public DomainProcessEngine getObject() throws Exception {
        if (engine == null) {
            Map<String, ProcessEngine> engines = new HashMap<>();

            for (Map.Entry<String, DataSource> entry : ctx.getBeansOfType(DataSource.class).entrySet()) {
                if (!entry.getKey().startsWith("local")) {
                    String domain = StringUtils.substringBefore(entry.getKey(), DataSource.class.getSimpleName());
                    DataSource dataSource = entry.getValue();
                    PlatformTransactionManager transactionManager = ctx.getBean(
                            domain + "TransactionManager", PlatformTransactionManager.class);
                    Object entityManagerFactory = ctx.getBean(domain + "EntityManagerFactory");

                    SpringProcessEngineConfiguration conf = ctx.getBean(SpringProcessEngineConfiguration.class);
                    conf.setDataSource(dataSource);
                    conf.setTransactionManager(transactionManager);
                    conf.setTransactionsExternallyManaged(true);
                    conf.setJpaEntityManagerFactory(entityManagerFactory);
                    if (conf.getBeans() == null) {
                        conf.setBeans(new SpringBeanFactoryProxyMap(ctx));
                    }
                    if (conf.getExpressionManager() == null) {
                        conf.setExpressionManager(new SpringExpressionManager(ctx, conf.getBeans()));
                    }

                    engines.put(domain, conf.buildProcessEngine());
                }
            }

            engine = new DomainProcessEngine(engines);
        }

        return engine;
    }

    @Override
    public Class<DomainProcessEngine> getObjectType() {
        return DomainProcessEngine.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        if (engine != null) {
            engine.close();
        }
    }

}
