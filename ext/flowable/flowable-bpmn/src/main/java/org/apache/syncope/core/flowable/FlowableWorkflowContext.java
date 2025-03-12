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
import org.apache.syncope.core.flowable.api.UserRequestHandler;
import org.apache.syncope.core.flowable.impl.FlowableBpmnProcessManager;
import org.apache.syncope.core.flowable.impl.FlowableUserRequestHandler;
import org.apache.syncope.core.flowable.impl.FlowableUserWorkflowAdapter;
import org.apache.syncope.core.flowable.impl.FlowableWorkflowUtils;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
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
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.rules.RuleProvider;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@EnableConfigurationProperties(WorkflowFlowableProperties.class)
@Configuration(proxyBeanMethods = false)
public class FlowableWorkflowContext {

    @ConditionalOnMissingBean
    @Bean
    public SpringIdmEngineConfiguration syncopeIdmEngineConfiguration(final ConfigurableApplicationContext ctx) {
        SpringIdmEngineConfiguration conf = new SpringIdmEngineConfiguration();
        conf.setIdmIdentityService(new SyncopeIdmIdentityService(conf, ctx));
        return conf;
    }

    @ConditionalOnMissingBean
    @Bean
    public SpringIdmEngineConfigurator syncopeIdmEngineConfigurator(
            final SpringIdmEngineConfiguration syncopeIdmEngineConfiguration) {
        SpringIdmEngineConfigurator configurator = new SpringIdmEngineConfigurator();
        configurator.setIdmEngineConfiguration(syncopeIdmEngineConfiguration);
        return configurator;
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeFormHandlerHelper syncopeFormHandlerHelper() {
        return new SyncopeFormHandlerHelper();
    }

    @ConditionalOnMissingBean
    @Bean
    public FlowableBpmnProcessManager bpmnProcessManager(final DomainProcessEngine engine) {
        return new FlowableBpmnProcessManager(engine);
    }

    @ConditionalOnMissingBean
    @Bean
    public FlowableUserRequestHandler userRequestHandler(
            final SecurityProperties securityProperties,
            final UserDataBinder userDataBinder,
            final DomainProcessEngine engine,
            final UserDAO userDAO,
            final EntityFactory entityFactory) {

        return new FlowableUserRequestHandler(
                userDataBinder,
                securityProperties.getAdminUser(),
                engine,
                userDAO,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public FlowableWorkflowUtils flowableUtils(final DomainProcessEngine engine) {
        return new FlowableWorkflowUtils(engine);
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
    public SpringProcessEngineConfiguration processEngineConfiguration(
            final WorkflowFlowableProperties props,
            final SpringIdmEngineConfigurator syncopeIdmEngineConfigurator,
            final IdGenerator idGenerator,
            final SyncopeEntitiesVariableType syncopeEntitiesVariableType,
            final SyncopeFormHandlerHelper syncopeFormHandlerHelper) {

        SpringProcessEngineConfiguration conf = new SpringProcessEngineConfiguration();
        conf.setDatabaseSchemaUpdate(AbstractEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        conf.setJpaHandleTransaction(true);
        conf.setJpaCloseEntityManager(false);
        conf.setHistoryLevel(props.getHistoryLevel());
        conf.setIdmEngineConfigurator(syncopeIdmEngineConfigurator);
        conf.setCustomPreVariableTypes(List.of(syncopeEntitiesVariableType));
        conf.setFormHandlerHelper(syncopeFormHandlerHelper);
        conf.setIdGenerator(idGenerator);
        conf.setPreBpmnParseHandlers(List.of(new ShellServiceTaskDisablingBpmnParseHandler()));
        return conf;
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainProcessEngineFactoryBean domainProcessEngineFactoryBean(final ConfigurableApplicationContext ctx) {
        return new DomainProcessEngineFactoryBean(ctx);
    }

    @Bean
    public Resource userWorkflowDef(final WorkflowFlowableProperties props,
            final ResourceLoader resourceLoader) {
        return resourceLoader.getResource(props.getUserWorkflowDef());
    }

    @ConditionalOnMissingBean(name = "flowableUWFAdapter")
    @Bean
    public UserWorkflowAdapter uwfAdapter(
            final UserDataBinder userDataBinder,
            final UserDAO userDAO,
            final RealmDAO realmDAO,
            final GroupDAO groupDAO,
            final EntityFactory entityFactory,
            final SecurityProperties securityProperties,
            final RuleProvider ruleProvider,
            final DomainProcessEngine engine,
            final UserRequestHandler userRequestHandler,
            final ApplicationEventPublisher publisher,
            final EncryptorManager encryptorManager) {

        return new FlowableUserWorkflowAdapter(
                userDataBinder,
                userDAO,
                realmDAO,
                groupDAO,
                entityFactory,
                securityProperties,
                ruleProvider,
                engine,
                userRequestHandler,
                publisher,
                encryptorManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AutoActivate autoActivate(final UserDataBinder userDataBinder, final UserDAO userDAO) {
        return new AutoActivate(userDataBinder, userDAO);
    }

    @ConditionalOnMissingBean
    @Bean
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
    public GenerateToken generateToken(final ConfParamOps confParamOps) {
        return new GenerateToken(confParamOps);
    }

    @ConditionalOnMissingBean
    @Bean
    public Notify notify(final NotificationManager notificationManager) {
        return new Notify(notificationManager);
    }

    @ConditionalOnMissingBean
    @Bean
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
    public Update update(final UserDataBinder userDataBinder, final UserDAO userDAO) {
        return new Update(userDataBinder, userDAO);
    }
}
