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

import java.time.Instant;
import java.util.Map;
import org.apache.syncope.wa.bootstrap.WAProperties;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.starter.config.WARefreshContextJob;
import org.apereo.cas.config.CasGoogleAuthenticatorLdapAutoConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.CasConfigurationPropertiesValidator;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {
    /*
     * List of Spring Boot classes that we want to disable and remove from auto-configuration.
     */
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
    RedisRepositoriesAutoConfiguration.class,
    CasGoogleAuthenticatorLdapAutoConfiguration.class
})
@EnableConfigurationProperties({ WAProperties.class, CasConfigurationProperties.class })
@EnableAsync(proxyTargetClass = false)
@EnableAspectJAutoProxy(proxyTargetClass = false)
@EnableTransactionManagement(proxyTargetClass = false)
@EnableScheduling
public class SyncopeWAApplication extends SpringBootServletInitializer {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeWAApplication.class);

    public static void main(final String[] args) {
        new SpringApplicationBuilder(SyncopeWAApplication.class).
                properties(Map.of("spring.config.name", "wa", "spring.cloud.bootstrap.name", "wa")).
                build().run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder builder) {
        return builder.properties(Map.of("spring.config.name", "wa",
                "spring.cloud.bootstrap.name", "wa")).sources(SyncopeWAApplication.class);
    }

    /**
     * Handle application ready event.
     *
     * @param event the event
     */
    @EventListener
    public void handleApplicationReadyEvent(final ApplicationReadyEvent event) {
        new CasConfigurationPropertiesValidator(event.getApplicationContext()).validate();

        WARestClient waRestClient = event.getApplicationContext().getBean(WARestClient.class);
        ContextRefresher contextRefresher = event.getApplicationContext().getBean(ContextRefresher.class);
        SamlIdPMetadataGenerator metadataGenerator =
                event.getApplicationContext().getBean(SamlIdPMetadataGenerator.class);

        WARefreshContextJob job = new WARefreshContextJob(waRestClient, contextRefresher, metadataGenerator);

        WAProperties waProperties = event.getApplicationContext().getBean(WAProperties.class);
        TaskScheduler scheduler = event.getApplicationContext().getBean(TaskScheduler.class);
        Instant startAt = Instant.now().plusSeconds(waProperties.getContextRefreshDelay());
        try {
            scheduler.schedule(job, startAt);
            LOG.info("Scheduled job to refresh application context @ [{}]", startAt);
        } catch (TaskRejectedException e) {
            throw new IllegalStateException("Could not schedule refresh job", e);
        }
    }
}
