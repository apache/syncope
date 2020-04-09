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
package org.apache.syncope.wa.starter;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.CasConfigurationPropertiesValidator;
import org.apereo.cas.util.AsciiArtUtils;
import org.apereo.cas.util.DateTimeUtils;

import org.apache.commons.lang.StringUtils;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@PropertySource("classpath:wa.properties")
@PropertySource(value = "file:${conf.directory}/wa.properties", ignoreResourceNotFound = true)
@SpringBootApplication(exclude = {
    HibernateJpaAutoConfiguration.class,
    JerseyAutoConfiguration.class,
    GroovyTemplateAutoConfiguration.class,
    GsonAutoConfiguration.class,
    JmxAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    RedisAutoConfiguration.class,
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    CassandraAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@EnableConfigurationProperties(CasConfigurationProperties.class)
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement(proxyTargetClass = true)
@EnableScheduling
public class SyncopeWAApplication extends SpringBootServletInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWAApplication.class);

    @Autowired
    private ContextRefresher contextRefresher;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Value("${contextRefreshDelay:15}")
    private long contextRefreshDelay;

    public static void main(final String[] args) {
        new SpringApplicationBuilder(SyncopeWAApplication.class).run(args);
    }

    private static void advertiseReady(final ApplicationReadyEvent event) {
        AsciiArtUtils.printAsciiArtReady(LOG, StringUtils.EMPTY);
        LOG.info("Ready to process requests @ [{}]", DateTimeUtils.zonedDateTimeOf(event.getTimestamp()));
    }

    private static void validateConfiguration(final ApplicationReadyEvent event) {
        if (!Boolean.getBoolean("SKIP_CONFIG_VALIDATION")) {
            CasConfigurationPropertiesValidator validator =
                new CasConfigurationPropertiesValidator(event.getApplicationContext());
            validator.validate();
        }
    }

    /**
     * Handle application ready event.
     *
     * @param event the event
     */
    @EventListener
    public void handleApplicationReadyEvent(final ApplicationReadyEvent event) {
        validateConfiguration(event);
        scheduleJobToRefreshContext();
        advertiseReady(event);
    }

    private void scheduleJobToRefreshContext() {
        try {
            Date date = Date.from(LocalDateTime.now().plusSeconds(this.contextRefreshDelay).
                atZone(ZoneId.systemDefault()).toInstant());
            Trigger trigger = TriggerBuilder.newTrigger().startAt(date).build();
            JobKey jobKey = new JobKey(getClass().getSimpleName());

            JobDetail job = JobBuilder.newJob(RefreshApplicationContextJob.class).withIdentity(jobKey).build();
            scheduler.getScheduler().scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException("Could not schedule refresh job", e);
        }
    }

    private class RefreshApplicationContextJob implements Job {
        @Override
        public void execute(final JobExecutionContext jobExecutionContext) {
            try {
                LOG.debug("Refreshed context: {}", contextRefresher.refresh());
            } catch (final Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
