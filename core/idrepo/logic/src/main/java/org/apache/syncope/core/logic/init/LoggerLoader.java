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
package org.apache.syncope.core.logic.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.logic.audit.AuditAppender;
import org.apache.syncope.core.logic.MemoryAppender;
import org.apache.syncope.core.logic.audit.JdbcAuditAppender;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Component;

@Component
public class LoggerLoader implements SyncopeCoreLoader {

    @Autowired
    private LoggerAccessor loggerAccessor;

    @Autowired
    private ImplementationLookup implementationLookup;

    @Value("${enable.jdbcAuditAppender:true}")
    private boolean enableJdbcAuditAppender;

    private final Map<String, MemoryAppender> memoryAppenders = new HashMap<>();

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

        ctx.getConfiguration().getAppenders().entrySet().stream().
                filter(entry -> entry.getValue() instanceof MemoryAppender).
                forEach(entry -> memoryAppenders.put(entry.getKey(), (MemoryAppender) entry.getValue()));

        if (enableJdbcAuditAppender) {
            JdbcAuditAppender jdbcAuditAppender = (JdbcAuditAppender) ApplicationContextProvider.getBeanFactory().
                    createBean(JdbcAuditAppender.class, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
            jdbcAuditAppender.init(domain);

            LoggerConfig logConf = new LoggerConfig(AuditLoggerName.getAuditLoggerName(domain), null, false);
            logConf.addAppender(jdbcAuditAppender.getTargetAppender(), Level.DEBUG, null);
            logConf.setLevel(Level.DEBUG);
            ctx.getConfiguration().addLogger(logConf.getName(), logConf);
        }

        // SYNCOPE-1144 For each custom audit appender class add related appenders to log4j logger
        auditAppenders(domain).forEach(auditAppender -> auditAppender.getEvents().stream().
                map(event -> AuditLoggerName.getAuditEventLoggerName(domain, event.toLoggerName())).
                forEach(domainAuditLoggerName -> {
                    LoggerConfig eventLogConf = ctx.getConfiguration().getLoggerConfig(domainAuditLoggerName);
                    boolean isRootLogConf = LogManager.ROOT_LOGGER_NAME.equals(eventLogConf.getName());
                    if (isRootLogConf) {
                        eventLogConf = new LoggerConfig(domainAuditLoggerName, null, false);
                    }
                    addAppenderToContext(ctx, auditAppender, eventLogConf);
                    eventLogConf.setLevel(Level.DEBUG);
                    if (isRootLogConf) {
                        ctx.getConfiguration().addLogger(domainAuditLoggerName, eventLogConf);
                    }
                }));

        AuthContextUtils.callAsAdmin(domain, () -> {
            loggerAccessor.synchronizeLog4J(ctx);
            return null;
        });

        ctx.updateLoggers();
    }

    public Map<String, MemoryAppender> getMemoryAppenders() {
        return memoryAppenders;
    }

    public List<AuditAppender> auditAppenders(final String domain) throws BeansException {
        return implementationLookup.getAuditAppenderClasses().stream().map(clazz -> {
            AuditAppender auditAppender;
            if (ApplicationContextProvider.getBeanFactory().containsSingleton(clazz.getName())) {
                auditAppender = (AuditAppender) ApplicationContextProvider.getBeanFactory().
                        getSingleton(clazz.getName());
            } else {
                auditAppender = (AuditAppender) ApplicationContextProvider.getBeanFactory().
                        createBean(clazz, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
                auditAppender.init(domain);
            }
            return auditAppender;
        }).collect(Collectors.toList());
    }

    public static void addAppenderToContext(
            final LoggerContext ctx,
            final AuditAppender auditAppender,
            final LoggerConfig eventLogConf) {

        Appender targetAppender = ctx.getConfiguration().getAppender(auditAppender.getTargetAppenderName());
        if (targetAppender == null) {
            targetAppender = auditAppender.getTargetAppender();
        }
        targetAppender.start();
        ctx.getConfiguration().addAppender(targetAppender);

        Optional<RewriteAppender> rewriteAppender = auditAppender.getRewriteAppender();
        if (rewriteAppender.isPresent()) {
            rewriteAppender.get().start();
            eventLogConf.addAppender(rewriteAppender.get(), Level.DEBUG, null);
        } else {
            eventLogConf.addAppender(targetAppender, Level.DEBUG, null);
        }
    }
}
