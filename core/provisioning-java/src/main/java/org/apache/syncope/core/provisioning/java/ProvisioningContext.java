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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.LogOutputStream;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.dao.ReportTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.auth.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.dao.auth.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyTypeClassDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyTypeDataBinder;
import org.apache.syncope.core.provisioning.api.data.ApplicationDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.provisioning.api.data.DelegationDataBinder;
import org.apache.syncope.core.provisioning.api.data.DynRealmDataBinder;
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
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.NotificationJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.cache.CaffeineVirAttrCache;
import org.apache.syncope.core.provisioning.java.data.AccessTokenDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AnyObjectDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AnyTypeClassDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AnyTypeDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.ApplicationDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AuditDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AuthModuleDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.AuthProfileDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.ClientAppDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.ConnInstanceDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.DelegationDataBinderImpl;
import org.apache.syncope.core.provisioning.java.data.DynRealmDataBinderImpl;
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
import org.apache.syncope.core.provisioning.java.job.AutowiringSpringBeanJobFactory;
import org.apache.syncope.core.provisioning.java.job.DefaultJobManager;
import org.apache.syncope.core.provisioning.java.job.SchedulerDBInit;
import org.apache.syncope.core.provisioning.java.job.SchedulerShutdown;
import org.apache.syncope.core.provisioning.java.job.SystemLoadReporterJob;
import org.apache.syncope.core.provisioning.java.job.notification.DefaultNotificationJobDelegate;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.apache.syncope.core.provisioning.java.job.report.DefaultReportJobDelegate;
import org.apache.syncope.core.provisioning.java.notification.DefaultNotificationManager;
import org.apache.syncope.core.provisioning.java.propagation.DefaultPropagationManager;
import org.apache.syncope.core.provisioning.java.propagation.PriorityPropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.DefaultCredentialChecker;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSSigner;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@EnableAsync
@EnableConfigurationProperties(ProvisioningProperties.class)
@Configuration
public class ProvisioningContext implements AsyncConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(ProvisioningContext.class);

    @Resource(name = "MasterDataSource")
    private DataSource masterDataSource;

    @Resource(name = "MasterTransactionManager")
    private PlatformTransactionManager masterTransactionManager;

    @Autowired
    private ProvisioningProperties provisioningProperties;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private AnySearchDAO anySearchDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private ApplicationDAO applicationDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private DynRealmDAO dynRealmDAO;

    @Autowired
    private ExternalResourceDAO externalResourceDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private PlainAttrValueDAO plainAttrValueDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ReportExecDAO reportExecDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Autowired
    private TaskUtilsFactory taskUtilsFactory;

    @Autowired
    private PasswordGenerator passwordGenerator;

    @Autowired
    private ConfParamOps confParamOps;

    @Autowired
    private SearchCondVisitor searchCondVisitor;

    @Autowired
    private DomainHolder domainHolder;

    @Autowired
    private ApplicationContext ctx;

    @ConditionalOnMissingBean
    @Bean
    public AsyncConnectorFacade asyncConnectorFacade() {
        return new AsyncConnectorFacade();
    }

    /**
     * Annotated as {@code @Primary} because it will be used by {@code @Async} in {@link AsyncConnectorFacade}.
     *
     * @return executor
     */
    @Bean
    @Primary
    public Executor asyncConnectorFacadeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(provisioningProperties.getAsyncConnectorFacadeExecutor().getCorePoolSize());
        executor.setMaxPoolSize(provisioningProperties.getAsyncConnectorFacadeExecutor().getMaxPoolSize());
        executor.setQueueCapacity(provisioningProperties.getAsyncConnectorFacadeExecutor().getQueueCapacity());
        executor.setThreadNamePrefix("AsyncConnectorFacadeExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return asyncConnectorFacadeExecutor();
    }

    /**
     * Used by {@link org.apache.syncope.core.provisioning.java.propagation.PriorityPropagationTaskExecutor}.
     *
     * @return executor
     */
    @Bean
    public ThreadPoolTaskExecutor propagationTaskExecutorAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(provisioningProperties.getPropagationTaskExecutorAsyncExecutor().getCorePoolSize());
        executor.setMaxPoolSize(provisioningProperties.getPropagationTaskExecutorAsyncExecutor().getMaxPoolSize());
        executor.setQueueCapacity(provisioningProperties.getPropagationTaskExecutorAsyncExecutor().getQueueCapacity());
        executor.setThreadNamePrefix("PropagationTaskExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public SchedulerDBInit quartzDataSourceInit() throws JsonProcessingException {
        SchedulerDBInit init = new SchedulerDBInit();
        init.setDataSource(masterDataSource);

        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.setContinueOnError(true);
        databasePopulator.setIgnoreFailedDrops(true);
        databasePopulator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        databasePopulator.setScripts(new ClassPathResource("/quartz/" + provisioningProperties.getQuartz().getSql()));
        init.setDatabasePopulator(databasePopulator);

        return init;
    }

    @DependsOn("quartzDataSourceInit")
    @Lazy(false)
    @Bean
    public SchedulerFactoryBean scheduler() {
        SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
        scheduler.setAutoStartup(true);
        scheduler.setApplicationContext(ctx);
        scheduler.setWaitForJobsToCompleteOnShutdown(true);
        scheduler.setOverwriteExistingJobs(true);
        scheduler.setDataSource(masterDataSource);
        scheduler.setTransactionManager(masterTransactionManager);
        scheduler.setJobFactory(new AutowiringSpringBeanJobFactory());

        Properties quartzProperties = new Properties();
        quartzProperties.setProperty(
                "org.quartz.scheduler.idleWaitTime",
                String.valueOf(provisioningProperties.getQuartz().getIdleWaitTime()));
        quartzProperties.setProperty(
                "org.quartz.jobStore.misfireThreshold",
                String.valueOf(provisioningProperties.getQuartz().getMisfireThreshold()));
        quartzProperties.setProperty(
                "org.quartz.jobStore.driverDelegateClass",
                provisioningProperties.getQuartz().getDelegate().getName());
        quartzProperties.setProperty("org.quartz.jobStore.isClustered", "true");
        quartzProperties.setProperty("org.quartz.jobStore.clusterCheckinInterval", "20000");
        quartzProperties.setProperty("org.quartz.scheduler.instanceName", "ClusteredScheduler");
        quartzProperties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        quartzProperties.setProperty("org.quartz.scheduler.jmx.export", "true");
        scheduler.setQuartzProperties(quartzProperties);

        return scheduler;
    }

    @Bean
    public SchedulerShutdown schedulerShutdown() {
        return new SchedulerShutdown(ctx);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public JobManager jobManager(final SchedulerFactoryBean scheduler) {
        DefaultJobManager jobManager = new DefaultJobManager(
                domainHolder,
                scheduler,
                taskDAO,
                reportDAO,
                implementationDAO,
                confParamOps,
                securityProperties);
        jobManager.setDisableQuartzInstance(provisioningProperties.getQuartz().isDisableInstance());
        return jobManager;
    }

    @ConditionalOnMissingBean
    @Bean
    public JavaMailSender mailSender() throws IllegalArgumentException, NamingException, IOException {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl() {

            @Override
            protected Transport connectTransport() throws MessagingException {
                // ensure that no auth means no auth
                if (StringUtils.isBlank(getUsername())) {
                    Transport transport = getTransport(getSession());
                    transport.connect(getHost(), getPort(), null, null);
                    return transport;
                }

                return super.connectTransport();
            }
        };
        mailSender.setDefaultEncoding(provisioningProperties.getSmtp().getDefaultEncoding());
        mailSender.setHost(provisioningProperties.getSmtp().getHost());
        mailSender.setPort(provisioningProperties.getSmtp().getPort());
        mailSender.setUsername(provisioningProperties.getSmtp().getUsername());
        mailSender.setPassword(provisioningProperties.getSmtp().getPassword());
        mailSender.setProtocol(provisioningProperties.getSmtp().getProtocol());

        if (LOG.isDebugEnabled()) {
            LOG.debug("[Mail] host:port = {}:{}", mailSender.getHost(), mailSender.getPort());
            LOG.debug("[Mail] protocol = {}", mailSender.getProtocol());
            LOG.debug("[Mail] username = {}", mailSender.getUsername());
            LOG.debug("[Mail] default encoding = {}", mailSender.getDefaultEncoding());
        }

        JndiObjectFactoryBean mailSession = new JndiObjectFactoryBean();
        mailSession.setJndiName("mail/syncopeNotification");
        try {
            mailSession.afterPropertiesSet();
        } catch (NamingException e) {
            LOG.debug("While looking up JNDI for mail session", e);
        }

        Session session = (Session) mailSession.getObject();
        if (session == null) {
            Properties javaMailProperties = mailSender.getJavaMailProperties();

            provisioningProperties.getSmtp().getJavamailProperties().
                    forEach((key, value) -> javaMailProperties.setProperty(key, value));

            if (StringUtils.isNotBlank(mailSender.getUsername())) {
                javaMailProperties.setProperty("mail.smtp.auth", "true");
            }

            if (LOG.isDebugEnabled()) {
                mailSender.getJavaMailProperties().
                        forEach((key, value) -> LOG.debug("[Mail] property: {} = {}", key, value));
            }

            if (provisioningProperties.getSmtp().isDebug()) {
                session = mailSender.getSession();
                session.setDebug(true);
                try (LogOutputStream los = new LogOutputStream(LOG)) {
                    session.setDebugOut(new PrintStream(los));
                }
            }
        } else {
            mailSender.setSession(session);
        }

        return mailSender;
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ConnectorManager connectorManager(
            final ConnIdBundleManager connIdBundleManager,
            final RealmDAO realmDAO,
            final ExternalResourceDAO resourceDAO,
            final ConnInstanceDataBinder connInstanceDataBinder,
            final AsyncConnectorFacade asyncConnectorFacade,
            final EntityFactory entityFactory) {

        return new DefaultConnectorManager(
                connIdBundleManager,
                realmDAO,
                externalResourceDAO,
                connInstanceDataBinder,
                asyncConnectorFacade,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ConnectorLoader connectorLoader(final ConnectorManager connectorManager) {
        return new ConnectorLoader(connectorManager);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public InboundMatcher inboundMatcher(
            final VirAttrHandler virAttrHandler,
            final IntAttrNameParser intAttrNameParser) {

        return new InboundMatcher(
                userDAO,
                anyObjectDAO,
                groupDAO,
                anySearchDAO,
                realmDAO,
                virSchemaDAO,
                virAttrHandler,
                intAttrNameParser,
                anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public OutboundMatcher outboundMatcher(
            final MappingManager mappingManager,
            final VirAttrHandler virAttrHandler) {

        return new OutboundMatcher(mappingManager, userDAO, anyUtilsFactory, virSchemaDAO, virAttrHandler);
    }

    @ConditionalOnMissingBean
    @Bean
    public DerAttrHandler derAttrHandler() {
        return new DefaultDerAttrHandler(anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public VirAttrHandler virAttrHandler(
            final ConnectorManager connectorManager,
            final VirAttrCache virAttrCache,
            @Lazy final OutboundMatcher outboundMatcher) {

        return new DefaultVirAttrHandler(connectorManager, virAttrCache, outboundMatcher, anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public MappingManager mappingManager(
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final VirAttrCache virAttrCache,
            final IntAttrNameParser intAttrNameParser) {

        return new DefaultMappingManager(
                anyTypeDAO,
                userDAO,
                anyObjectDAO,
                groupDAO,
                relationshipTypeDAO,
                realmDAO,
                applicationDAO,
                derAttrHandler,
                virAttrHandler,
                virAttrCache,
                passwordGenerator,
                anyUtilsFactory,
                intAttrNameParser);
    }

    @ConditionalOnMissingBean
    @Bean
    public TemplateUtils templateUtils() {
        return new TemplateUtils(userDAO, groupDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ConnObjectUtils connObjectUtils(
            final MappingManager mappingManager,
            final TemplateUtils templateUtils) {

        return new ConnObjectUtils(
                templateUtils,
                realmDAO,
                userDAO,
                externalResourceDAO,
                passwordGenerator,
                mappingManager,
                anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public PropagationManager propagationManager(
            final VirSchemaDAO virSchemaDAO,
            final ConnObjectUtils connObjectUtils,
            final MappingManager mappingManager,
            final DerAttrHandler derAttrHandler) {

        return new DefaultPropagationManager(
                virSchemaDAO,
                externalResourceDAO,
                entityFactory,
                connObjectUtils,
                mappingManager,
                derAttrHandler,
                anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnIdBundleManager connIdBundleManager() {
        return new DefaultConnIdBundleManager(provisioningProperties.getConnIdLocation());
    }

    @ConditionalOnMissingBean
    @Bean
    public IntAttrNameParser intAttrNameParser() {
        return new IntAttrNameParser(plainSchemaDAO, derSchemaDAO, virSchemaDAO, anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public PropagationTaskExecutor propagationTaskExecutor(
            final ConnectorManager connectorManager,
            final ConnObjectUtils connObjectUtils,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final TaskDAO taskDAO,
            final NotificationManager notificationManager,
            final AuditManager auditManager,
            final TaskDataBinder taskDataBinder,
            final OutboundMatcher outboundMatcher) {

        return new PriorityPropagationTaskExecutor(
                connectorManager,
                connObjectUtils,
                userDAO,
                groupDAO,
                anyObjectDAO,
                taskDAO,
                externalResourceDAO,
                notificationManager,
                auditManager,
                taskDataBinder,
                anyUtilsFactory,
                taskUtilsFactory,
                entityFactory,
                outboundMatcher,
                propagationTaskExecutorAsyncExecutor());
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public UserProvisioningManager userProvisioningManager(
            final UserWorkflowAdapter uwfAdapter,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final VirAttrHandler virtAttrHandler) {

        return new DefaultUserProvisioningManager(
                uwfAdapter,
                propagationManager,
                taskExecutor,
                virtAttrHandler,
                userDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public GroupProvisioningManager groupProvisioningManager(
            final GroupWorkflowAdapter gwfAdapter,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final GroupDataBinder groupDataBinder,
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
    @Autowired
    public AnyObjectProvisioningManager anyObjectProvisioningManager(
            final AnyObjectWorkflowAdapter awfAdapter,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final VirAttrHandler virtAttrHandler) {

        return new DefaultAnyObjectProvisioningManager(
                awfAdapter,
                propagationManager,
                taskExecutor,
                virtAttrHandler,
                anyObjectDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public VirAttrCache virAttrCache() {
        VirAttrCache virAttrCache = new CaffeineVirAttrCache();
        virAttrCache.setCacheSpec(provisioningProperties.getVirAttrCacheSpec());
        return virAttrCache;
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public NotificationManager notificationManager(
            final NotificationDAO notificationDAO,
            final AnyMatchDAO anyMatchDAO,
            final UserDataBinder userDataBinder,
            final GroupDataBinder groupDataBinder,
            final AnyObjectDataBinder anyObjectDataBinder,
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

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public AuditManager auditManager(final AuditConfDAO auditConfDAO) {
        return new DefaultAuditManager(auditConfDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public SystemLoadReporterJob systemLoadReporterJob() {
        return new SystemLoadReporterJob(ctx);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public NotificationJobDelegate notificationJobDelegate(
            final JavaMailSender mailSender,
            final AuditManager auditManager,
            final NotificationManager notificationManager) {

        return new DefaultNotificationJobDelegate(
                taskDAO,
                mailSender,
                entityFactory,
                auditManager,
                notificationManager);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public NotificationJob notificationJob(final NotificationJobDelegate delegate) {
        return new NotificationJob(securityProperties, domainHolder, delegate);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportJobDelegate reportJobDelegate() {
        return new DefaultReportJobDelegate(reportDAO, reportExecDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public AccessTokenDataBinder accessTokenDataBinder(
            final AccessTokenJWSSigner jwsSigner,
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
    @Autowired
    public AnyObjectDataBinder anyObjectDataBinder(
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher) {

        return new AnyObjectDataBinderImpl(
                anyTypeDAO,
                realmDAO,
                anyTypeClassDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                plainSchemaDAO,
                plainAttrDAO,
                plainAttrValueDAO,
                externalResourceDAO,
                relationshipTypeDAO,
                entityFactory,
                anyUtilsFactory,
                derAttrHandler,
                virAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassDataBinder anyTypeClassDataBinder() {
        return new AnyTypeClassDataBinderImpl(plainSchemaDAO, derSchemaDAO, virSchemaDAO, anyTypeDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeDataBinder anyTypeDataBinder() {
        return new AnyTypeDataBinderImpl(
                securityProperties,
                anyTypeDAO,
                anyTypeClassDAO,
                accessTokenDAO,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ApplicationDataBinder applicationDataBinder() {
        return new ApplicationDataBinderImpl(applicationDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditDataBinder auditDataBinder() {
        return new AuditDataBinderImpl();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleDataBinder authModuleDataBinder() {
        return new AuthModuleDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthProfileDataBinder authProfileDataBinder() {
        return new AuthProfileDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ClientAppDataBinder clientAppDataBinder() {
        return new ClientAppDataBinderImpl(policyDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ConnInstanceDataBinder connInstanceDataBinder(final ConnIdBundleManager connIdBundleManager) {
        return new ConnInstanceDataBinderImpl(connIdBundleManager, connInstanceDAO, realmDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationDataBinder delegationDataBinder() {
        return new DelegationDataBinderImpl(userDAO, roleDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public DynRealmDataBinder dynRealmDataBinder() {
        return new DynRealmDataBinderImpl(anyTypeDAO, dynRealmDAO, entityFactory, searchCondVisitor);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public GroupDataBinder groupDataBinder(
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher) {

        return new GroupDataBinderImpl(
                anyTypeDAO,
                realmDAO,
                anyTypeClassDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                plainSchemaDAO,
                plainAttrDAO,
                plainAttrValueDAO,
                externalResourceDAO,
                relationshipTypeDAO,
                entityFactory,
                anyUtilsFactory,
                derAttrHandler,
                virAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher,
                searchCondVisitor);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationDataBinder implementationDataBinder() {
        return new ImplementationDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public NotificationDataBinder notificationDataBinder(
            final MailTemplateDAO mailTemplateDAO,
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
    public OIDCJWKSDataBinder oidcJWKSDataBinder() {
        return new OIDCJWKSDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public PolicyDataBinder policyDataBinder() {
        return new PolicyDataBinderImpl(externalResourceDAO, realmDAO, anyTypeDAO, implementationDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmDataBinder realmDataBinder() {
        return new RealmDataBinderImpl(
                anyTypeDAO,
                implementationDAO,
                realmDAO,
                policyDAO,
                externalResourceDAO,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeDataBinder relationshipTypeDataBinder() {
        return new RelationshipTypeDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationDataBinder remediationDataBinder() {
        return new RemediationDataBinderImpl();
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ReportDataBinder reportDataBinder(
            final ReportTemplateDAO reportTemplateDAO,
            final SchedulerFactoryBean scheduler) {

        return new ReportDataBinderImpl(reportTemplateDAO, reportExecDAO, implementationDAO, scheduler);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ResourceDataBinder resourceDataBinder(final IntAttrNameParser intAttrNameParser) {
        return new ResourceDataBinderImpl(
                anyTypeDAO,
                connInstanceDAO,
                policyDAO,
                virSchemaDAO,
                anyTypeClassDAO,
                implementationDAO,
                plainSchemaDAO,
                entityFactory,
                intAttrNameParser);
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleDataBinder roleDataBinder() {
        return new RoleDataBinderImpl(realmDAO, dynRealmDAO, roleDAO, applicationDAO, entityFactory, searchCondVisitor);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2IdPEntityDataBinder saml2IdPEntityDataBinder() {
        return new SAML2IdPEntityDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPEntityDataBinder saml2SPEntityDataBinder() {
        return new SAML2SPEntityDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public SRARouteDataBinder sraRouteDataBinder() {
        return new SRARouteDataBinderImpl();
    }

    @ConditionalOnMissingBean
    @Bean
    public SchemaDataBinder schemaDataBinder() {
        return new SchemaDataBinderImpl(
                anyTypeClassDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                externalResourceDAO,
                anyTypeDAO,
                implementationDAO,
                entityFactory,
                anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionDataBinder securityQuestionDataBinder() {
        return new SecurityQuestionDataBinderImpl(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public TaskDataBinder taskDataBinder(
            final TaskExecDAO taskExecDAO,
            final SchedulerFactoryBean scheduler) {

        return new TaskDataBinderImpl(
                realmDAO,
                externalResourceDAO,
                taskExecDAO,
                anyTypeDAO,
                implementationDAO,
                entityFactory,
                scheduler,
                taskUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public UserDataBinder userDataBinder(
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher,
            final SecurityQuestionDAO securityQuestionDAO,
            final DelegationDAO delegationDAO) {

        return new UserDataBinderImpl(
                anyTypeDAO,
                realmDAO,
                anyTypeClassDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                plainSchemaDAO,
                plainAttrDAO,
                plainAttrValueDAO,
                externalResourceDAO,
                relationshipTypeDAO,
                entityFactory,
                anyUtilsFactory,
                derAttrHandler,
                virAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher,
                searchCondVisitor,
                roleDAO,
                securityQuestionDAO,
                applicationDAO,
                accessTokenDAO,
                delegationDAO,
                confParamOps,
                securityProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public WAConfigDataBinder waConfigDataBinder(final WAConfigDAO waConfigDAO) {
        return new WAConfigDataBinderImpl(waConfigDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public WAClientAppDataBinder waClientAppDataBinder(
            final ClientAppDataBinder clientAppDataBinder,
            final PolicyDataBinder policyDataBinder,
            final AuthModuleDAO authModuleDAO) {

        return new WAClientAppDataBinderImpl(clientAppDataBinder, policyDataBinder, authModuleDAO);
    }
}
