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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.db.ColumnMapping;
import org.apache.logging.log4j.core.appender.db.jdbc.ColumnConfig;
import org.apache.logging.log4j.core.appender.db.jdbc.ConnectionSource;
import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.logic.AuditAppender;
import org.apache.syncope.core.logic.MemoryAppender;
import org.apache.syncope.core.provisioning.java.AuditManagerImpl;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

@Component
public class LoggerLoader implements SyncopeLoader {

    @Autowired
    private DomainsHolder domainsHolder;

    @Autowired
    private LoggerAccessor loggerAccessor;

    @Autowired
    private ImplementationLookup implementationLookup;

    private final Map<String, MemoryAppender> memoryAppenders = new HashMap<>();

    @Override
    public Integer getPriority() {
        return 300;
    }

    @Override
    public void load() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

        for (Map.Entry<String, Appender> entry : ctx.getConfiguration().getAppenders().entrySet()) {
            if (entry.getValue() instanceof MemoryAppender) {
                memoryAppenders.put(entry.getKey(), (MemoryAppender) entry.getValue());
            }
        }

        // Audit table and DataSource for each configured domain
        ColumnConfig[] columnConfigs = {
            ColumnConfig.newBuilder().
            setConfiguration(ctx.getConfiguration()).setName("EVENT_DATE").setEventTimestamp(true).build(),
            ColumnConfig.newBuilder().setUnicode(false).
            setConfiguration(ctx.getConfiguration()).setName("LOGGER_LEVEL").setPattern("%level").build(),
            ColumnConfig.newBuilder().setUnicode(false).
            setConfiguration(ctx.getConfiguration()).setName("LOGGER").setPattern("%logger").build(),
            ColumnConfig.newBuilder().setUnicode(false).
            setConfiguration(ctx.getConfiguration()).setName("MESSAGE").setPattern("%message").build(),
            ColumnConfig.newBuilder().setUnicode(false).
            setConfiguration(ctx.getConfiguration()).setName("THROWABLE").setPattern("%ex{full}").build()
        };
        ColumnMapping[] columnMappings = new ColumnMapping[0];

        for (Map.Entry<String, DataSource> entry : domainsHolder.getDomains().entrySet()) {
            Appender appender = ctx.getConfiguration().getAppender("audit_for_" + entry.getKey());
            if (appender == null) {
                appender = JdbcAppender.newBuilder().
                        withName("audit_for_" + entry.getKey()).
                        withIgnoreExceptions(false).
                        setConnectionSource(new DataSourceConnectionSource(entry.getValue())).
                        setBufferSize(0).
                        setTableName("SYNCOPEAUDIT").
                        setColumnConfigs(columnConfigs).
                        setColumnMappings(columnMappings).
                        build();
                appender.start();
                ctx.getConfiguration().addAppender(appender);
            }

            LoggerConfig logConf = new LoggerConfig(
                    AuditManagerImpl.getDomainAuditLoggerName(entry.getKey()), null, false);
            logConf.addAppender(appender, Level.DEBUG, null);
            logConf.setLevel(Level.DEBUG);
            ctx.getConfiguration().addLogger(AuditManagerImpl.getDomainAuditLoggerName(entry.getKey()), logConf);

            // SYNCOPE-1144 For each custom audit appender class add related appenders to log4j logger
            configureCustomAppenders(entry.getKey(), ctx);

            AuthContextUtils.execWithAuthContext(entry.getKey(), new AuthContextUtils.Executable<Void>() {

                @Override
                public Void exec() {
                    loggerAccessor.synchronizeLog4J(ctx);
                    return null;
                }
            });
        }

        ctx.updateLoggers();
    }

    public Map<String, MemoryAppender> getMemoryAppenders() {
        return memoryAppenders;
    }

    public void configureCustomAppenders(final String domainName, final LoggerContext ctx) {
        List<AuditAppender> auditAppenders = auditAppenders(domainName);
        for (AuditAppender auditAppender : auditAppenders) {
            for (AuditLoggerName event : auditAppender.getEvents()) {
                String domainAuditLoggerName =
                        AuditManagerImpl.getDomainAuditEventLoggerName(domainName, event.toLoggerName());
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
            }
        }
    }

    public List<AuditAppender> auditAppenders(final String domainName) throws BeansException {
        List<AuditAppender> auditAppenders = new ArrayList<>();
        for (Class<?> clazz : implementationLookup.getAuditAppenderClasses()) {
            AuditAppender auditAppender;
            if (ApplicationContextProvider.getBeanFactory().containsSingleton(clazz.getName())) {
                auditAppender = (AuditAppender) ApplicationContextProvider.getBeanFactory().
                        getSingleton(clazz.getName());
            } else {
                auditAppender = (AuditAppender) ApplicationContextProvider.getBeanFactory().
                        createBean(clazz, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
                auditAppender.setDomainName(domainName);
                auditAppender.init();
            }
            auditAppenders.add(auditAppender);
        }
        return auditAppenders;
    }

    public void addAppenderToContext(
            final LoggerContext ctx,
            final AuditAppender auditAppender,
            final LoggerConfig eventLogConf) {
        Appender targetAppender = ctx.getConfiguration().getAppender(auditAppender.getTargetAppenderName());
        if (targetAppender == null) {
            targetAppender = auditAppender.getTargetAppender();
        }
        targetAppender.start();
        ctx.getConfiguration().addAppender(targetAppender);
        if (auditAppender.isRewriteEnabled()) {
            RewriteAppender rewriteAppender = ctx.getConfiguration().getAppender(auditAppender.
                    getTargetAppenderName() + "_rewrite");
            if (rewriteAppender == null) {
                rewriteAppender = auditAppender.getRewriteAppender();
            }
            rewriteAppender.start();
            ctx.getConfiguration().addAppender(rewriteAppender);
            eventLogConf.addAppender(rewriteAppender, Level.DEBUG, null);
        } else {
            eventLogConf.addAppender(targetAppender, Level.DEBUG, null);
        }
    }

    private static class DataSourceConnectionSource implements ConnectionSource {

        private final DataSource dataSource;

        DataSourceConnectionSource(final DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DataSourceUtils.getConnection(dataSource);
        }

    }
}
