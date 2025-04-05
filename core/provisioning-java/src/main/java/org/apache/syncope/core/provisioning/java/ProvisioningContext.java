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

import java.util.List;
import java.util.concurrent.Executor;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
import org.apache.syncope.core.persistence.api.dao.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.AuditEventProcessor;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyTypeClassDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyTypeDataBinder;
import org.apache.syncope.core.provisioning.api.data.AttrRepoDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.provisioning.api.data.DelegationDataBinder;
import org.apache.syncope.core.provisioning.api.data.DynRealmDataBinder;
import org.apache.syncope.core.provisioning.api.data.FIQLQueryDataBinder;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.ImplementationDataBinder;
import org.apache.syncope.core.provisioning.api.data.NotificationDataBinder;
import org.apache.syncope.core.provisioning.api.data.OIDCJWKSDataBinder;
import org.apache.syncope.core.provisioning.api.data.PolicyDataBinder;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;
import org.apache.syncope.core.provisioning.api.data.RemediationDataBinder;
import org.apache.syncope.core.provisioning.api.data.ReportDataBinder;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.core.provisioning.api.data.RoleDataBinder;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPEntityDataBinder;
import org.apache.syncope.core.provisioning.api.data.SAML2SPEntityDataBinder;
import org.apache.syncope.core.provisioning.api.data.SRARouteDataBinder;
import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.apache.syncope.core.provisioning.api.data.SecurityQuestionDataBinder;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.data.WAConfigDataBinder;
import org.apache.syncope.core.provisioning.api.data.wa.WAClientAppDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.notification.NotificationJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.cache.VirAttrCacheKey;
import org.apache.syncope.core.provisioning.java.cache.VirAttrCacheValue;
import org.apache.syncope.core.provisioning.java.data.AccessTokenDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AnyObjectDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AnyTypeClassDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AnyTypeDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AttrRepoDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AuditDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AuthModuleDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AuthProfileDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.ClientAppDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.ConnInstanceDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.DelegationDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.DynRealmDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.FIQLQueryDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.GroupDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.ImplementationDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.NotificationDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.OIDCJWKSDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.PolicyDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.RealmDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.RelationshipTypeDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.RemediationDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.ReportDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.ResourceDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.RoleDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.SAML2IdPEntityDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.SAML2SPEntityDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.SRARouteDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.SchemaDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.SecurityQuestionDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.TaskDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.UserDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.WAConfigDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.wa.WAClientAppDataBinderImpl;
import org.apache.syncope.core.provisioning.java.job.DefaultJobManager;
import org.apache.syncope.core.provisioning.java.job.JobStatusUpdater;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.provisioning.java.job.SystemLoadReporterJob;
import org.apache.syncope.core.provisioning.java.job.notification.MailNotificationJobDelegate;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.apache.syncope.core.provisioning.java.notification.DefaultNotificationManager;
import org.apache.syncope.core.provisioning.java.propagation.DefaultPropagationManager;
import org.apache.syncope.core.provisioning.java.propagation.PriorityPropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.pushpull.LiveSyncTaskSaver;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.DefaultCredentialChecker;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSSigner;
import org.apache.syncope.core.spring.task.VirtualThreadPoolTaskExecutor;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

@EnableAsync
@EnableConfigurationProperties(ProvisioningProperties.class)
@Configuration(proxyBeanMethods = false)
public class ProvisioningContext {

    @ConditionalOnMissingBean
    @Bean
    public AsyncConnectorFacade asyncConnectorFacade() {
        return new AsyncConnectorFacade();
    }

    /**
     * Annotated as {@code @Primary} because it will be used by {@code @Async} in {@link AsyncConnectorFacade}.
     *
     * @param props configuration properties
     * @return executor
     */
    @Bean
    @Primary
    public VirtualThreadPoolTaskExecutor asyncConnectorFacadeExecutor(final ProvisioningProperties props) {
        VirtualThreadPoolTaskExecutor executor = new VirtualThreadPoolTaskExecutor();
        executor.setPoolSize(props.getAsyncConnectorFacadeExecutor().getPoolSize());
        executor.setAwaitTerminationSeconds(props.getAsyncConnectorFacadeExecutor().getAwaitTerminationSeconds());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("AsyncConnectorFacadeExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public AsyncConfigurer asyncConfigurer(
            @Qualifier("asyncConnectorFacadeExecutor")
            final VirtualThreadPoolTaskExecutor asyncConnectorFacadeExecutor) {

        return new AsyncConfigurer() {

            @Override
            public Executor getAsyncExecutor() {
                return asyncConnectorFacadeExecutor;
            }
        };
    }

    /**
     * Used by {@link org.apache.syncope.core.provisioning.java.propagation.PriorityPropagationTaskExecutor}.
     *
     * @param props the provisioning properties
     * @return executor thread pool task executor
     */
    @Bean
    public VirtualThreadPoolTaskExecutor propagationTaskExecutorAsyncExecutor(final ProvisioningProperties props) {
        VirtualThreadPoolTaskExecutor executor = new VirtualThreadPoolTaskExecutor();
        executor.setPoolSize(props.getPropagationTaskExecutorAsyncExecutor().getPoolSize());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(
                props.getPropagationTaskExecutorAsyncExecutor().getAwaitTerminationSeconds());
        executor.setThreadNamePrefix("PropagationTaskExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public SyncopeTaskScheduler taskScheduler(final ProvisioningProperties props, final JobStatusDAO jobStatusDAO) {
        SimpleAsyncTaskScheduler taskScheduler = new SimpleAsyncTaskScheduler();
        taskScheduler.setVirtualThreads(true);
        taskScheduler.setConcurrencyLimit(props.getScheduling().getPoolSize());
        taskScheduler.setTaskTerminationTimeout(props.getScheduling().getAwaitTerminationSeconds() * 1000);
        taskScheduler.setThreadNamePrefix("TaskScheduler-");

        return new SyncopeTaskScheduler(taskScheduler, jobStatusDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public JobManager jobManager(
            final DomainHolder<?> domainHolder,
            final SecurityProperties securityProperties,
            final SyncopeTaskScheduler scheduler,
            final JobStatusDAO jobStatusDAO,
            final TaskDAO taskDAO,
            final ReportDAO reportDAO,
            final ImplementationDAO implementationDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final ConfParamOps confParamOps) {

        return new DefaultJobManager(
                domainHolder,
                scheduler,
                jobStatusDAO,
                taskDAO,
                reportDAO,
                implementationDAO,
                taskUtilsFactory,
                confParamOps,
                securityProperties);
    }

    /**
     * This is a special thread executor that only created a single worker thread.
     * This is necessary to allow job status update operations to queue up serially
     * and not via multiple threads to avoid the "lost update" problem.
     *
     * @return the async task executor
     */
    @Bean
    public AsyncTaskExecutor jobStatusUpdaterThreadExecutor() {
        VirtualThreadPoolTaskExecutor executor = new VirtualThreadPoolTaskExecutor();
        executor.setPoolSize(1);
        executor.setThreadNamePrefix("JobStatusUpdaterThreadExecutor-");
        executor.initialize();
        return executor;
    }

    @ConditionalOnMissingBean
    @Bean
    public JobStatusUpdater jobStatusUpdater(final JobStatusDAO jobStatusDAO, final EntityFactory entityFactory) {
        return new JobStatusUpdater(jobStatusDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnectorManager connectorManager(
            final EntityFactory entityFactory,
            final ConnIdBundleManager connIdBundleManager,
            final RealmDAO realmDAO,
            final RealmSearchDAO realmSearchDAO,
            final ExternalResourceDAO resourceDAO,
            final ConnInstanceDataBinder connInstanceDataBinder,
            final AsyncConnectorFacade asyncConnectorFacade) {

        return new DefaultConnectorManager(
                connIdBundleManager,
                realmDAO,
                realmSearchDAO,
                resourceDAO,
                connInstanceDataBinder,
                asyncConnectorFacade,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnectorLoader connectorLoader(final ConnectorManager connectorManager) {
        return new ConnectorLoader(connectorManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public InboundMatcher inboundMatcher(
            final AnyUtilsFactory anyUtilsFactory,
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO anySearchDAO,
            final RealmDAO realmDAO,
            final RealmSearchDAO realmSearchDAO,
            final VirSchemaDAO virSchemaDAO,
            final ImplementationDAO implementationDAO,
            final VirAttrHandler virAttrHandler,
            final IntAttrNameParser intAttrNameParser) {

        return new InboundMatcher(
                userDAO,
                anyObjectDAO,
                groupDAO,
                anySearchDAO,
                realmDAO,
                realmSearchDAO,
                virSchemaDAO,
                implementationDAO,
                virAttrHandler,
                intAttrNameParser,
                anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public OutboundMatcher outboundMatcher(
            final AnyUtilsFactory anyUtilsFactory,
            final MappingManager mappingManager,
            final UserDAO userDAO,
            final VirSchemaDAO virSchemaDAO,
            final VirAttrHandler virAttrHandler) {

        return new OutboundMatcher(mappingManager, userDAO, anyUtilsFactory, virSchemaDAO, virAttrHandler);
    }

    @ConditionalOnMissingBean
    @Bean
    public DerAttrHandler derAttrHandler(final AnyUtilsFactory anyUtilsFactory) {
        return new DefaultDerAttrHandler(anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public VirAttrHandler virAttrHandler(
            final AnyUtilsFactory anyUtilsFactory,
            final ConnectorManager connectorManager,
            final Cache<VirAttrCacheKey, VirAttrCacheValue> virAttrCache,
            @Lazy final OutboundMatcher outboundMatcher) {

        return new DefaultVirAttrHandler(connectorManager, virAttrCache, outboundMatcher, anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public MappingManager mappingManager(
            final AnyTypeDAO anyTypeDAO,
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final GroupDAO groupDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final RealmSearchDAO realmSearchDAO,
            final ImplementationDAO implementationDAO,
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final Cache<VirAttrCacheKey, VirAttrCacheValue> virAttrCache,
            final IntAttrNameParser intAttrNameParser,
            final EncryptorManager encryptorManager) {

        return new DefaultMappingManager(
                anyTypeDAO,
                userDAO,
                anyObjectDAO,
                groupDAO,
                relationshipTypeDAO,
                realmSearchDAO,
                implementationDAO,
                derAttrHandler,
                virAttrHandler,
                virAttrCache,
                intAttrNameParser,
                encryptorManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public TemplateUtils templateUtils(final UserDAO userDAO, final GroupDAO groupDAO) {
        return new TemplateUtils(userDAO, groupDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnObjectUtils connObjectUtils(
            final PasswordGenerator passwordGenerator,
            final AnyUtilsFactory anyUtilsFactory,
            final MappingManager mappingManager,
            final TemplateUtils templateUtils,
            final RealmSearchDAO realmSearchDAO,
            final UserDAO userDAO,
            final ExternalResourceDAO resourceDAO,
            final EncryptorManager encryptorManager) {

        return new ConnObjectUtils(
                templateUtils,
                realmSearchDAO,
                userDAO,
                resourceDAO,
                passwordGenerator,
                mappingManager,
                anyUtilsFactory,
                encryptorManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public PropagationManager propagationManager(
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final VirSchemaDAO virSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final ConnObjectUtils connObjectUtils,
            final MappingManager mappingManager,
            final DerAttrHandler derAttrHandler) {

        return new DefaultPropagationManager(
                virSchemaDAO,
                resourceDAO,
                entityFactory,
                connObjectUtils,
                mappingManager,
                derAttrHandler,
                anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnIdBundleManager connIdBundleManager(final ProvisioningProperties props) {
        return new DefaultConnIdBundleManager(props.getConnIdLocation());
    }

    @ConditionalOnMissingBean
    @Bean
    public IntAttrNameParser intAttrNameParser(
            final AnyUtilsFactory anyUtilsFactory,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO) {

        return new IntAttrNameParser(plainSchemaDAO, derSchemaDAO, virSchemaDAO, anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public PropagationTaskExecutor propagationTaskExecutor(
            @Qualifier("propagationTaskExecutorAsyncExecutor")
            final VirtualThreadPoolTaskExecutor propagationTaskExecutorAsyncExecutor,
            final TaskUtilsFactory taskUtilsFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final ConnectorManager connectorManager,
            final ConnObjectUtils connObjectUtils,
            final TaskDAO taskDAO,
            final ExternalResourceDAO resourceDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final NotificationManager notificationManager,
            final AuditManager auditManager,
            final TaskDataBinder taskDataBinder,
            final OutboundMatcher outboundMatcher,
            final PlainAttrValidationManager validator,
            final ApplicationEventPublisher publisher) {

        return new PriorityPropagationTaskExecutor(
                connectorManager,
                connObjectUtils,
                taskDAO,
                resourceDAO,
                plainSchemaDAO,
                notificationManager,
                auditManager,
                taskDataBinder,
                anyUtilsFactory,
                taskUtilsFactory,
                outboundMatcher,
                validator,
                publisher,
                propagationTaskExecutorAsyncExecutor);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserProvisioningManager userProvisioningManager(
            final UserWorkflowAdapter uwfAdapter,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final UserDAO userDAO,
            final VirAttrHandler virtAttrHandler) {

        return new DefaultUserProvisioningManager(
                uwfAdapter,
                propagationManager,
                taskExecutor,
                userDAO,
                virtAttrHandler);
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupProvisioningManager groupProvisioningManager(
            final GroupWorkflowAdapter gwfAdapter,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final GroupDataBinder groupDataBinder,
            final GroupDAO groupDAO,
            final VirAttrHandler virtAttrHandler) {

        return new DefaultGroupProvisioningManager(
                gwfAdapter,
                propagationManager,
                taskExecutor,
                groupDataBinder,
                groupDAO,
                virtAttrHandler);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectProvisioningManager anyObjectProvisioningManager(
            final AnyObjectWorkflowAdapter awfAdapter,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final AnyObjectDAO anyObjectDAO,
            final VirAttrHandler virtAttrHandler) {

        return new DefaultAnyObjectProvisioningManager(
                awfAdapter,
                propagationManager,
                taskExecutor,
                anyObjectDAO,
                virtAttrHandler);
    }

    @ConditionalOnMissingBean(name = VirAttrHandler.CACHE)
    @Bean(name = VirAttrHandler.CACHE)
    public Cache<VirAttrCacheKey, VirAttrCacheValue> virAttrCache(final CacheManager cacheManager) {
        return cacheManager.createCache(VirAttrHandler.CACHE,
                new MutableConfiguration<VirAttrCacheKey, VirAttrCacheValue>().
                        setTypes(VirAttrCacheKey.class, VirAttrCacheValue.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.FIVE_MINUTES)));
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationManager notificationManager(
            final EntityFactory entityFactory,
            final SearchCondVisitor searchCondVisitor,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final NotificationDAO notificationDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO anySearchDAO,
            final AnyMatchDAO anyMatchDAO,
            final TaskDAO taskDAO,
            final UserDataBinder userDataBinder,
            final GroupDataBinder groupDataBinder,
            final AnyObjectDataBinder anyObjectDataBinder,
            final ConfParamOps confParamOps,
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final IntAttrNameParser intAttrNameParser) {

        return new DefaultNotificationManager(
                derSchemaDAO,
                virSchemaDAO,
                notificationDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                anySearchDAO,
                anyMatchDAO,
                taskDAO,
                derAttrHandler,
                virAttrHandler,
                userDataBinder,
                groupDataBinder,
                anyObjectDataBinder,
                confParamOps,
                entityFactory,
                intAttrNameParser,
                searchCondVisitor);
    }

    /**
     * This is a special thread executor to allow audit event reports.
     *
     * @return the async task executor
     */
    @Bean
    public AsyncTaskExecutor auditManagerThreadExecutor() {
        VirtualThreadPoolTaskExecutor executor = new VirtualThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("AuditManagerThreadExecutor-");
        executor.initialize();
        return executor;
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditManager auditManager(
            final AuditConfDAO auditConfDAO,
            final AuditEventDAO auditEventDAO,
            final EntityFactory entityFactory,
            final List<AuditEventProcessor> auditEventProcessors,
            @Qualifier("auditManagerThreadExecutor")
            final AsyncTaskExecutor taskExecutor) {

        return new DefaultAuditManager(auditConfDAO, auditEventDAO, entityFactory, auditEventProcessors, taskExecutor);
    }

    @ConditionalOnMissingBean
    @Bean
    public SystemLoadReporterJob systemLoadReporterJob(final ApplicationContext ctx) {
        return new SystemLoadReporterJob(ctx);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationJobDelegate notificationJobDelegate(
            final TaskUtilsFactory taskUtilsFactory,
            final TaskDAO taskDAO,
            final AuditManager auditManager,
            final NotificationManager notificationManager,
            final ApplicationEventPublisher publisher,
            final JavaMailSender mailSender) {

        return new MailNotificationJobDelegate(
                taskDAO,
                taskUtilsFactory,
                auditManager,
                notificationManager,
                publisher,
                mailSender);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationJob notificationJob(
            final NotificationJobDelegate delegate,
            final DomainHolder<?> domainHolder,
            final SecurityProperties securityProperties) {

        return new NotificationJob(securityProperties, domainHolder, delegate);
    }

    @ConditionalOnMissingBean
    @Bean
    public LiveSyncTaskSaver liveSyncTaskExecSaver(
            final ExternalResourceDAO resourceDAO,
            final TaskDAO taskDAO,
            final TaskExecDAO taskExecDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final NotificationManager notificationManager,
            final AuditManager auditManager) {

        return new LiveSyncTaskSaver(
                resourceDAO,
                taskDAO,
                taskExecDAO,
                taskUtilsFactory,
                notificationManager,
                auditManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenDataBinder accessTokenDataBinder(
            final EntityFactory entityFactory,
            final SecurityProperties securityProperties,
            final AccessTokenJWSSigner jwsSigner,
            final AccessTokenDAO accessTokenDAO,
            final ConfParamOps confParamOps,
            final DefaultCredentialChecker credentialChecker) {

        return new AccessTokenDataBinderImpl(
                securityProperties,
                jwsSigner,
                accessTokenDAO,
                confParamOps,
                entityFactory,
                credentialChecker);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectDataBinder anyObjectDataBinder(
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final AnyTypeDAO anyTypeDAO,
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher,
            final PlainAttrValidationManager validator) {

        return new AnyObjectDataBinderImpl(
                anyTypeDAO,
                realmSearchDAO,
                anyTypeClassDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                plainSchemaDAO,
                resourceDAO,
                relationshipTypeDAO,
                entityFactory,
                anyUtilsFactory,
                derAttrHandler,
                virAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher,
                validator);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassDataBinder anyTypeClassDataBinder(
            final EntityFactory entityFactory,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final AnyTypeDAO anyTypeDAO) {

        return new AnyTypeClassDataBinderImpl(plainSchemaDAO, derSchemaDAO, virSchemaDAO, anyTypeDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeDataBinder anyTypeDataBinder(
            final EntityFactory entityFactory,
            final SecurityProperties securityProperties,
            final EncryptorManager encryptorManager,
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final AccessTokenDAO accessTokenDAO) {

        return new AnyTypeDataBinderImpl(
                securityProperties,
                encryptorManager,
                anyTypeDAO,
                anyTypeClassDAO,
                accessTokenDAO,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditDataBinder auditDataBinder() {
        return new AuditDataBinderImpl();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleDataBinder authModuleDataBinder(final EntityFactory entityFactory) {
        return new AuthModuleDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AttrRepoDataBinder attrRepoDataBinder(final EntityFactory entityFactory) {
        return new AttrRepoDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthProfileDataBinder authProfileDataBinder(final EntityFactory entityFactory) {
        return new AuthProfileDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ClientAppDataBinder clientAppDataBinder(
            final PolicyDAO policyDAO,
            final RealmSearchDAO realmSearchDAO,
            final EntityFactory entityFactory) {

        return new ClientAppDataBinderImpl(policyDAO, realmSearchDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnInstanceDataBinder connInstanceDataBinder(
            final EntityFactory entityFactory,
            final ConnIdBundleManager connIdBundleManager,
            final ConnInstanceDAO connInstanceDAO,
            final RealmSearchDAO realmSearchDAO) {

        return new ConnInstanceDataBinderImpl(connIdBundleManager, connInstanceDAO, realmSearchDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationDataBinder delegationDataBinder(
            final UserDAO userDAO,
            final RoleDAO roleDAO,
            final EntityFactory entityFactory) {

        return new DelegationDataBinderImpl(userDAO, roleDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public FIQLQueryDataBinder fiqlQueryDataBinder(
            final SearchCondVisitor searchCondVisitor,
            final UserDAO userDAO,
            final EntityFactory entityFactory) {

        return new FIQLQueryDataBinderImpl(searchCondVisitor, userDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public DynRealmDataBinder dynRealmDataBinder(
            final AnyTypeDAO anyTypeDAO,
            final DynRealmDAO dynRealmDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityFactory entityFactory) {

        return new DynRealmDataBinderImpl(anyTypeDAO, dynRealmDAO, entityFactory, searchCondVisitor);
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupDataBinder groupDataBinder(
            final EntityFactory entityFactory,
            final SearchCondVisitor searchCondVisitor,
            final AnyUtilsFactory anyUtilsFactory,
            final AnyTypeDAO anyTypeDAO,
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher,
            final PlainAttrValidationManager validator) {

        return new GroupDataBinderImpl(
                anyTypeDAO,
                realmSearchDAO,
                anyTypeClassDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                plainSchemaDAO,
                resourceDAO,
                relationshipTypeDAO,
                entityFactory,
                anyUtilsFactory,
                derAttrHandler,
                virAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher,
                searchCondVisitor,
                validator);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationDataBinder implementationDataBinder(final EntityFactory entityFactory) {
        return new ImplementationDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationDataBinder notificationDataBinder(
            final EntityFactory entityFactory,
            final MailTemplateDAO mailTemplateDAO,
            final AnyTypeDAO anyTypeDAO,
            final ImplementationDAO implementationDAO,
            final IntAttrNameParser intAttrNameParser) {

        return new NotificationDataBinderImpl(
                mailTemplateDAO,
                anyTypeDAO,
                implementationDAO,
                entityFactory,
                intAttrNameParser);

    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCJWKSDataBinder oidcJWKSDataBinder(final EntityFactory entityFactory) {
        return new OIDCJWKSDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public PolicyDataBinder policyDataBinder(
            final EntityFactory entityFactory,
            final ExternalResourceDAO resourceDAO,
            final RealmDAO realmDAO,
            final AnyTypeDAO anyTypeDAO,
            final ImplementationDAO implementationDAO) {

        return new PolicyDataBinderImpl(resourceDAO, realmDAO, anyTypeDAO, implementationDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmDataBinder realmDataBinder(
            final EntityFactory entityFactory,
            final AnyTypeDAO anyTypeDAO,
            final ImplementationDAO implementationDAO,
            final RealmDAO realmDAO,
            final PolicyDAO policyDAO,
            final ExternalResourceDAO resourceDAO) {

        return new RealmDataBinderImpl(
                anyTypeDAO,
                implementationDAO,
                realmDAO,
                policyDAO,
                resourceDAO,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeDataBinder relationshipTypeDataBinder(
            final AnyTypeDAO anyTypeDAO,
            final EntityFactory entityFactory) {

        return new RelationshipTypeDataBinderImpl(anyTypeDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationDataBinder remediationDataBinder() {
        return new RemediationDataBinderImpl();
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportDataBinder reportDataBinder(
            final ReportExecDAO reportExecDAO,
            final ImplementationDAO implementationDAO,
            final SyncopeTaskScheduler scheduler) {

        return new ReportDataBinderImpl(reportExecDAO, implementationDAO, scheduler);
    }

    @ConditionalOnMissingBean
    @Bean
    public ResourceDataBinder resourceDataBinder(
            final EntityFactory entityFactory,
            final AnyTypeDAO anyTypeDAO,
            final ConnInstanceDAO connInstanceDAO,
            final PolicyDAO policyDAO,
            final VirSchemaDAO virSchemaDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final ImplementationDAO implementationDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final IntAttrNameParser intAttrNameParser,
            final PropagationTaskExecutor propagationTaskExecutor) {

        return new ResourceDataBinderImpl(
                anyTypeDAO,
                connInstanceDAO,
                policyDAO,
                virSchemaDAO,
                anyTypeClassDAO,
                implementationDAO,
                plainSchemaDAO,
                entityFactory,
                intAttrNameParser,
                propagationTaskExecutor);
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleDataBinder roleDataBinder(
            final EntityFactory entityFactory,
            final SearchCondVisitor searchCondVisitor,
            final RealmSearchDAO realmSearchDAO,
            final DynRealmDAO dynRealmDAO,
            final RoleDAO roleDAO) {

        return new RoleDataBinderImpl(
                realmSearchDAO,
                dynRealmDAO,
                roleDAO,
                entityFactory,
                searchCondVisitor);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2IdPEntityDataBinder saml2IdPEntityDataBinder(final EntityFactory entityFactory) {
        return new SAML2IdPEntityDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPEntityDataBinder saml2SPEntityDataBinder(final EntityFactory entityFactory) {
        return new SAML2SPEntityDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public SRARouteDataBinder sraRouteDataBinder() {
        return new SRARouteDataBinderImpl();
    }

    @ConditionalOnMissingBean
    @Bean
    public SchemaDataBinder schemaDataBinder(
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final AnyTypeClassDAO anyTypeClassDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final AnyTypeDAO anyTypeDAO,
            final ImplementationDAO implementationDAO) {

        return new SchemaDataBinderImpl(
                anyTypeClassDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                resourceDAO,
                anyTypeDAO,
                implementationDAO,
                entityFactory,
                anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionDataBinder securityQuestionDataBinder(final EntityFactory entityFactory) {
        return new SecurityQuestionDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskDataBinder taskDataBinder(
            final EntityFactory entityFactory,
            final TaskUtilsFactory taskUtilsFactory,
            final RealmSearchDAO realmSearchDAO,
            final ExternalResourceDAO resourceDAO,
            final TaskExecDAO taskExecDAO,
            final AnyTypeDAO anyTypeDAO,
            final ImplementationDAO implementationDAO,
            final SyncopeTaskScheduler scheduler) {

        return new TaskDataBinderImpl(
                realmSearchDAO,
                resourceDAO,
                taskExecDAO,
                anyTypeDAO,
                implementationDAO,
                entityFactory,
                scheduler,
                taskUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserDataBinder userDataBinder(
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final SecurityProperties securityProperties,
            final AnyTypeDAO anyTypeDAO,
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher,
            final PlainAttrValidationManager validator,
            final RoleDAO roleDAO,
            final SecurityQuestionDAO securityQuestionDAO,
            final AccessTokenDAO accessTokenDAO,
            final DelegationDAO delegationDAO,
            final ConfParamOps confParamOps) {

        return new UserDataBinderImpl(
                anyTypeDAO,
                realmSearchDAO,
                anyTypeClassDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                plainSchemaDAO,
                resourceDAO,
                relationshipTypeDAO,
                entityFactory,
                anyUtilsFactory,
                derAttrHandler,
                virAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher,
                validator,
                roleDAO,
                securityQuestionDAO,
                accessTokenDAO,
                delegationDAO,
                confParamOps,
                securityProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    public WAConfigDataBinder waConfigDataBinder(
            final WAConfigDAO waConfigDAO,
            final EntityFactory entityFactory) {

        return new WAConfigDataBinderImpl(waConfigDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public WAClientAppDataBinder waClientAppDataBinder(
            final ClientAppDataBinder clientAppDataBinder,
            final PolicyDataBinder policyDataBinder,
            final AuthModuleDataBinder authModuleDataBinder,
            final AuthModuleDAO authModuleDAO) {

        return new WAClientAppDataBinderImpl(
                clientAppDataBinder,
                policyDataBinder,
                authModuleDataBinder,
                authModuleDAO);
    }
}
