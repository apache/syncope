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
package org.apache.syncope.core.flowable.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.ProcessEngine;
import org.flowable.common.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.flowable.engine.impl.el.ProcessExpressionManager;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring factory for {@link DomainProcessEngine} which takes the provided {@link SpringProcessEngineConfiguration} as
 * template for each of the configured Syncope domains.
 */
@Component
public class DomainProcessEngineFactoryBean implements FactoryBean<DomainProcessEngine>, ApplicationContextAware {

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

            ctx.getBeansOfType(DataSource.class).forEach((key, dataSource) -> {
                if (!key.startsWith("local")) {
                    String domain = StringUtils.substringBefore(key, DataSource.class.getSimpleName());
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
                        conf.setExpressionManager(new ProcessExpressionManager(conf.getBeans()));
                    }
                    if (EngineServiceUtil.getIdmEngineConfiguration(conf) == null) {
                        SpringIdmEngineConfiguration spiec = ctx.getBean(SpringIdmEngineConfiguration.class);
                        conf.addEngineConfiguration(spiec.getEngineCfgKey(), spiec.getEngineScopeType(), spiec);
                    }
                    conf.setEnableSafeBpmnXml(true);
                    conf.setCustomFormTypes(Arrays.asList(new DropdownFormType(null), new PasswordFormType()));
                    conf.setDisableEventRegistry(true);

                    engines.put(domain, conf.buildProcessEngine());
                }
            });

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

    @PreDestroy
    public void preDestroy() {
        if (engine != null) {
            engine.close();
        }
    }
}
