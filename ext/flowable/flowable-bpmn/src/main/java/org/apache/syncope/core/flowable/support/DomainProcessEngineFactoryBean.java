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

import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.flowable.common.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.el.ProcessExpressionManager;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring factory for {@link DomainProcessEngine} which takes the provided {@link SpringProcessEngineConfiguration} as
 * template for each of the configured Syncope domains.
 */
public class DomainProcessEngineFactoryBean implements FactoryBean<DomainProcessEngine>, SyncopeCoreLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(DomainProcessEngineFactoryBean.class);

    protected final ApplicationContext ctx;

    protected DomainProcessEngine engine;

    public DomainProcessEngineFactoryBean(final ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int getOrder() {
        return 300;
    }

    protected ProcessEngine build(final DataSource datasource) {
        PlatformTransactionManager transactionManager = ctx.getBean(
                "domainTransactionManager", PlatformTransactionManager.class);

        SpringProcessEngineConfiguration conf = ctx.getBean(SpringProcessEngineConfiguration.class);
        conf.setDataSource(datasource);
        conf.setTransactionManager(transactionManager);
        conf.setTransactionsExternallyManaged(true);
        conf.setJpaEntityManagerFactory(ctx.getBean("entityManagerFactory"));
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
        conf.setCustomFormTypes(List.of(new DropdownFormType(null), new PasswordFormType()));
        conf.setDisableEventRegistry(true);

        return conf.buildProcessEngine();
    }

    @SuppressWarnings("unchecked")
    protected DomainHolder<DataSource> getDomainHolder() {
        return ctx.getBean(DomainHolder.class);
    }

    @Override
    public void load(final String domain) {
        try {
            getObject().getEngines().put(domain, build(getDomainHolder().getDomains().get(domain)));
        } catch (Exception e) {
            LOG.error("Could not setup Flowable for {}", domain, e);
        }
    }

    @Override
    public DomainProcessEngine getObject() {
        if (engine == null) {
            Map<String, ProcessEngine> engines = new HashMap<>();

            getDomainHolder().getDomains().forEach((domain, datasource) -> engines.put(domain, build(datasource)));

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
        Optional.ofNullable(engine).ifPresent(DomainProcessEngine::close);
    }
}
