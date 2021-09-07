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
package org.apache.syncope.core.flowable;

import java.util.List;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.flowable.impl.FlowableBpmnProcessManager;
import org.apache.syncope.core.flowable.impl.FlowableUserRequestHandler;
import org.apache.syncope.core.flowable.impl.FlowableWorkflowUtils;
import org.apache.syncope.core.flowable.support.DomainProcessEngineFactoryBean;
import org.apache.syncope.core.flowable.support.ShellServiceTaskDisablingBpmnParseHandler;
import org.apache.syncope.core.flowable.support.SyncopeEntitiesVariableType;
import org.apache.syncope.core.flowable.support.SyncopeFormHandlerHelper;
import org.apache.syncope.core.flowable.support.SyncopeIdmIdentityService;
import org.apache.syncope.core.flowable.task.AutoActivate;
import org.apache.syncope.core.flowable.task.Create;
import org.apache.syncope.core.flowable.task.Delete;
import org.apache.syncope.core.flowable.task.GenerateToken;
import org.apache.syncope.core.flowable.task.Notify;
import org.apache.syncope.core.flowable.task.PasswordReset;
import org.apache.syncope.core.flowable.task.Reactivate;
import org.apache.syncope.core.flowable.task.Suspend;
import org.apache.syncope.core.flowable.task.Update;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.workflow.java.WorkflowContext;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Import(WorkflowContext.class)
@EnableConfigurationProperties(WorkflowFlowableProperties.class)
@Configuration
public class WorkflowFlowableContext {

    @Autowired
    private WorkflowFlowableProperties props;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ConfigurableApplicationContext ctx;

    @ConditionalOnMissingBean
    @Bean
    public SpringIdmEngineConfiguration syncopeIdmEngineConfiguration() {
        SpringIdmEngineConfiguration conf = new SpringIdmEngineConfiguration();
        conf.setIdmIdentityService(new SyncopeIdmIdentityService(conf, ctx));
        return conf;
    }

    @ConditionalOnMissingBean
    @Bean
    public SpringIdmEngineConfigurator syncopeIdmEngineConfigurator() {
        SpringIdmEngineConfigurator configurator = new SpringIdmEngineConfigurator();
        configurator.setIdmEngineConfiguration(syncopeIdmEngineConfiguration());
        return configurator;
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeFormHandlerHelper syncopeFormHandlerHelper() {
        return new SyncopeFormHandlerHelper();
    }

    @ConditionalOnMissingBean
    @Bean
    public FlowableBpmnProcessManager bpmnProcessManager() {
        return new FlowableBpmnProcessManager();
    }

    @ConditionalOnMissingBean
    @Bean
    public FlowableUserRequestHandler userRequestHandler() {
        return new FlowableUserRequestHandler();
    }

    @ConditionalOnMissingBean
    @Bean
    public FlowableWorkflowUtils flowableUtils() {
        return new FlowableWorkflowUtils();
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeEntitiesVariableType syncopeEntitiesVariableType() {
        return new SyncopeEntitiesVariableType();
    }

    @ConditionalOnMissingBean
    @Bean
    public IdGenerator idGenerator() {
        return new StrongUuidGenerator();
    }

    @ConditionalOnMissingBean
    @Bean
    @Scope("prototype")
    public SpringProcessEngineConfiguration processEngineConfiguration() {
        SpringProcessEngineConfiguration conf = new SpringProcessEngineConfiguration();
        conf.setDatabaseSchemaUpdate(AbstractEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        conf.setJpaHandleTransaction(true);
        conf.setJpaCloseEntityManager(false);
        conf.setHistoryLevel(props.getHistoryLevel());
        conf.setIdmEngineConfigurator(syncopeIdmEngineConfigurator());
        conf.setCustomPreVariableTypes(List.of(syncopeEntitiesVariableType()));
        conf.setFormHandlerHelper(syncopeFormHandlerHelper());
        conf.setIdGenerator(idGenerator());
        conf.setPreBpmnParseHandlers(List.of(new ShellServiceTaskDisablingBpmnParseHandler()));
        return conf;
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainProcessEngineFactoryBean domainProcessEngineFactoryBean() {
        return new DomainProcessEngineFactoryBean(ctx);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public AutoActivate autoActivate(final UserDataBinder userDataBinder, final UserDAO userDAO) {
        return new AutoActivate(userDataBinder, userDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public Create create(final UserDataBinder userDataBinder, final EntityFactory entityFactory) {
        return new Create(userDataBinder, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public Delete delete() {
        return new Delete();
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public GenerateToken generateToken(final ConfParamOps confParamOps) {
        return new GenerateToken(confParamOps);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public Notify notify(final NotificationManager notificationManager) {
        return new Notify(notificationManager);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public PasswordReset passwordReset(final UserDataBinder userDataBinder, final UserDAO userDAO) {
        return new PasswordReset(userDataBinder, userDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public Reactivate reactivate() {
        return new Reactivate();
    }

    @ConditionalOnMissingBean
    @Bean
    public Suspend suspend() {
        return new Suspend();
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public Update update(final UserDataBinder userDataBinder, final UserDAO userDAO) {
        return new Update(userDataBinder, userDAO);
    }

    @Bean
    public Resource userWorkflowDef() {
        return resourceLoader.getResource(props.getUserWorkflowDef());
    }
}
