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
import java.lang.reflect.InvocationTargetException;
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
import org.apache.syncope.common.lib.LogOutputStream;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.job.AutowiringSpringBeanJobFactory;
import org.apache.syncope.core.provisioning.java.job.JobManagerImpl;
import org.apache.syncope.core.provisioning.java.job.SchedulerDBInit;
import org.apache.syncope.core.provisioning.java.job.SchedulerShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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

@ComponentScan("org.apache.syncope.core.provisioning.java")
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
    private ProvisioningProperties props;

    @Autowired
    private ApplicationContext ctx;

    /**
     * Annotated as {@code @Primary} because it will be used by {@code @Async} in {@link AsyncConnectorFacade}.
     *
     * @return executor
     */
    @Bean
    @Primary
    public Executor asyncConnectorFacadeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getAsyncConnectorFacadeExecutor().getCorePoolSize());
        executor.setMaxPoolSize(props.getAsyncConnectorFacadeExecutor().getMaxPoolSize());
        executor.setQueueCapacity(props.getAsyncConnectorFacadeExecutor().getQueueCapacity());
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
    public Executor propagationTaskExecutorAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getPropagationTaskExecutorAsyncExecutor().getCorePoolSize());
        executor.setMaxPoolSize(props.getPropagationTaskExecutorAsyncExecutor().getMaxPoolSize());
        executor.setQueueCapacity(props.getPropagationTaskExecutorAsyncExecutor().getQueueCapacity());
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
        databasePopulator.setScripts(new ClassPathResource("/quartz/" + props.getQuartz().getSql()));
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
                String.valueOf(props.getQuartz().getIdleWaitTime()));
        quartzProperties.setProperty(
                "org.quartz.jobStore.misfireThreshold",
                String.valueOf(props.getQuartz().getMisfireThreshold()));
        quartzProperties.setProperty(
                "org.quartz.jobStore.driverDelegateClass",
                props.getQuartz().getDelegate().getName());
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

    @Bean
    public JobManager jobManager() {
        JobManagerImpl jobManager = new JobManagerImpl();
        jobManager.setDisableQuartzInstance(props.getQuartz().isDisableInstance());
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
        mailSender.setDefaultEncoding(props.getSmtp().getDefaultEncoding());
        mailSender.setHost(props.getSmtp().getHost());
        mailSender.setPort(props.getSmtp().getPort());
        mailSender.setUsername(props.getSmtp().getUsername());
        mailSender.setPassword(props.getSmtp().getPassword());
        mailSender.setProtocol(props.getSmtp().getProtocol());

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

            props.getSmtp().getJavamailProperties().
                    forEach((key, value) -> javaMailProperties.setProperty(key, value));

            if (StringUtils.isNotBlank(mailSender.getUsername())) {
                javaMailProperties.setProperty("mail.smtp.auth", "true");
            }

            if (LOG.isDebugEnabled()) {
                mailSender.getJavaMailProperties().
                        forEach((key, value) -> LOG.debug("[Mail] property: {} = {}", key, value));
            }

            if (props.getSmtp().isDebug()) {
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
    public PropagationManager propagationManager() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return props.getPropagationManager().getDeclaredConstructor().newInstance();
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnIdBundleManager connIdBundleManager() {
        return new ConnIdBundleManagerImpl(props.getConnIdLocation());
    }

    @Bean
    public IntAttrNameParser intAttrNameParser() {
        return new IntAttrNameParser();
    }

    @Bean
    public PropagationTaskExecutor propagationTaskExecutor() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return props.getPropagationTaskExecutor().getDeclaredConstructor().newInstance();
    }

    @Bean
    public UserProvisioningManager userProvisioningManager() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return props.getUserProvisioningManager().getDeclaredConstructor().newInstance();
    }

    @Bean
    public GroupProvisioningManager groupProvisioningManager() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return props.getGroupProvisioningManager().getDeclaredConstructor().newInstance();
    }

    @Bean
    public AnyObjectProvisioningManager anyObjectProvisioningManager() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return props.getAnyObjectProvisioningManager().getDeclaredConstructor().newInstance();
    }

    @Bean
    public VirAttrCache virAttrCache() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        VirAttrCache virAttrCache = props.getVirAttrCache().getDeclaredConstructor().newInstance();
        virAttrCache.setCacheSpec(props.getVirAttrCacheSpec());
        return virAttrCache;
    }

    @Bean
    public NotificationManager notificationManager() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return props.getNotifcationManager().getDeclaredConstructor().newInstance();
    }

    @Bean
    public AuditManager auditManager() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return props.getAuditManager().getDeclaredConstructor().newInstance();
    }
}
