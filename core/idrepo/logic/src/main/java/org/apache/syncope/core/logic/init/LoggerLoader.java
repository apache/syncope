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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.db.ColumnMapping;
import org.apache.logging.log4j.core.appender.db.jdbc.AbstractConnectionSource;
import org.apache.logging.log4j.core.appender.db.jdbc.ColumnConfig;
import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.logic.audit.AuditAppender;
import org.apache.syncope.core.logic.MemoryAppender;
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
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

        ctx.getConfiguration().getAppenders().entrySet().stream().
                filter(entry -> (entry.getValue() instanceof MemoryAppender)).
                forEach(entry -> {
                    memoryAppenders.put(entry.getKey(), (MemoryAppender) entry.getValue());
                });

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
                        setConnectionSource(new DataSourceConnectionSource(entry.getKey(), entry.getValue())).
                        setBufferSize(0).
                        setTableName("SYNCOPEAUDIT").
                        setColumnConfigs(columnConfigs).
                        setColumnMappings(columnMappings).
                        build();
                appender.start();
                ctx.getConfiguration().addAppender(appender);
            }

            LoggerConfig logConf = new LoggerConfig(AuditLoggerName.getAuditLoggerName(entry.getKey()), null, false);
            logConf.addAppender(appender, Level.DEBUG, null);
            logConf.setLevel(Level.DEBUG);
            ctx.getConfiguration().addLogger(logConf.getName(), logConf);

            // SYNCOPE-1144 For each custom audit appender class add related appenders to log4j logger
            auditAppenders(entry.getKey()).forEach(auditAppender -> {
                auditAppender.getEvents().stream().
                        map(event -> AuditLoggerName.getAuditEventLoggerName(entry.getKey(), event.toLoggerName())).
                        forEachOrdered(domainAuditLoggerName -> {
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
                        });
            });

            AuthContextUtils.execWithAuthContext(entry.getKey(), () -> {
                loggerAccessor.synchronizeLog4J(ctx);
                return null;
            });
        }

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
                auditAppender.setDomainName(domain);
                auditAppender.init();
            }
            return auditAppender;
        }).collect(Collectors.toList());
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

    private static class DataSourceConnectionSource extends AbstractConnectionSource {

        private final String description;

        private final DataSource dataSource;

        DataSourceConnectionSource(final String domain, final DataSource dataSource) {
            this.description = "dataSource{ domain=" + domain + ", value=" + dataSource + " }";
            this.dataSource = dataSource;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DataSourceUtils.getConnection(dataSource);
        }

        @Override
        public String toString() {
            return this.description;
        }
    }
}
