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

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Resource;
import javax.sql.DataSource;
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
import org.apache.syncope.core.provisioning.java.propagation.PropagationManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@PropertySource("classpath:connid.properties")
@PropertySource("classpath:mail.properties")
@PropertySource("classpath:provisioning.properties")
@PropertySource(value = "file:${conf.directory}/connid.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:${conf.directory}/mail.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:${conf.directory}/provisioning.properties", ignoreResourceNotFound = true)
@ComponentScan("org.apache.syncope.core.provisioning.java")
@EnableAsync
@Configuration
public class ProvisioningContext implements EnvironmentAware, AsyncConfigurer {

    @Resource(name = "MasterDataSource")
    private DataSource masterDataSource;

    @Resource(name = "MasterTransactionManager")
    private PlatformTransactionManager masterTransactionManager;

    @Autowired
    private ApplicationContext ctx;

    private Environment env;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = env;
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
        executor.setCorePoolSize(env.getProperty("asyncConnectorFacadeExecutor.corePoolSize", Integer.class));
        executor.setMaxPoolSize(env.getProperty("asyncConnectorFacadeExecutor.maxPoolSize", Integer.class));
        executor.setQueueCapacity(env.getProperty("asyncConnectorFacadeExecutor.queueCapacity", Integer.class));
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
        executor.setCorePoolSize(env.getProperty("propagationTaskExecutorAsyncExecutor.corePoolSize", Integer.class));
        executor.setMaxPoolSize(env.getProperty("propagationTaskExecutorAsyncExecutor.maxPoolSize", Integer.class));
        executor.setQueueCapacity(env.getProperty("propagationTaskExecutorAsyncExecutor.queueCapacity", Integer.class));
        executor.setThreadNamePrefix("PropagationTaskExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public SchedulerDBInit quartzDataSourceInit() {
        SchedulerDBInit init = new SchedulerDBInit();
        init.setDataSource(masterDataSource);

        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.setContinueOnError(true);
        databasePopulator.setIgnoreFailedDrops(true);
        databasePopulator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        databasePopulator.setScripts(new ClassPathResource("/quartz/" + env.getProperty("quartz.sql")));
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
                "org.quartz.scheduler.idleWaitTime", env.getProperty("quartz.scheduler.idleWaitTime", "30000"));
        quartzProperties.setProperty(
                "org.quartz.jobStore.misfireThreshold", env.getProperty("quartz.misfireThreshold", "60000"));
        quartzProperties.setProperty("org.quartz.jobStore.driverDelegateClass", env.getProperty("quartz.jobstore"));
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
        jobManager.setDisableQuartzInstance(env.getProperty("quartz.disableInstance", Boolean.class, false));
        return jobManager;
    }

    @ConditionalOnMissingBean
    @Bean
    public JavaMailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setDefaultEncoding(env.getProperty("smtpEncoding"));
        mailSender.setHost(env.getProperty("smtpHost"));
        mailSender.setPort(env.getProperty("smtpPort", Integer.class));
        mailSender.setUsername(env.getProperty("smtpUsername"));
        mailSender.setPassword(env.getProperty("smtpPassword"));
        mailSender.setProtocol(env.getProperty("smtpProtocol"));
        return mailSender;
    }

    @ConditionalOnMissingBean
    @Bean
    public PropagationManager propagationManager() {
        return new PropagationManagerImpl();
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnIdBundleManager connIdBundleManager() {
        ConnIdBundleManagerImpl connIdBundleManager = new ConnIdBundleManagerImpl();
        connIdBundleManager.setStringLocations(env.getProperty("connid.locations"));
        return connIdBundleManager;
    }

    @Bean
    public IntAttrNameParser intAttrNameParser() {
        return new IntAttrNameParser();
    }

    @Bean
    public PropagationTaskExecutor propagationTaskExecutor()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (PropagationTaskExecutor) Class.forName(env.getProperty("propagationTaskExecutor")).
                getConstructor().newInstance();
    }

    @Bean
    public UserProvisioningManager userProvisioningManager()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (UserProvisioningManager) Class.forName(env.getProperty("userProvisioningManager")).
                getConstructor().newInstance();
    }

    @Bean
    public GroupProvisioningManager groupProvisioningManager()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (GroupProvisioningManager) Class.forName(env.getProperty("groupProvisioningManager")).
                getConstructor().newInstance();
    }

    @Bean
    public AnyObjectProvisioningManager anyObjectProvisioningManager()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (AnyObjectProvisioningManager) Class.forName(env.getProperty("anyObjectProvisioningManager")).
                getConstructor().newInstance();
    }

    @Bean
    public VirAttrCache virAttrCache()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (VirAttrCache) Class.forName(env.getProperty("virAttrCache")).
                getConstructor().newInstance();
    }

    @Bean
    public NotificationManager notificationManager()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (NotificationManager) Class.forName(env.getProperty("notificationManager")).
                getConstructor().newInstance();
    }

    @Bean
    public AuditManager auditManager()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (AuditManager) Class.forName(env.getProperty("auditManager")).
                getConstructor().newInstance();
    }
}
