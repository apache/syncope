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
import java.util.Map;
import javax.sql.DataSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.db.jdbc.ColumnConfig;
import org.apache.logging.log4j.core.appender.db.jdbc.ConnectionSource;
import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.core.misc.AuditManager;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.persistence.api.dao.LoggerDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LoggerLoader implements SyncopeLoader {

    @Autowired
    private DomainsHolder domainsHolder;

    @Autowired
    private LoggerDAO loggerDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public Integer getPriority() {
        return 300;
    }

    @Transactional
    @Override
    public void load() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

        // 1. Audit table and DataSource for each configured domain
        ColumnConfig[] columns = {
            ColumnConfig.createColumnConfig(ctx.getConfiguration(), "EVENT_DATE", null, null, "true", null, null),
            ColumnConfig.createColumnConfig(ctx.getConfiguration(), "LOGGER_LEVEL", "%level", null, null, null, null),
            ColumnConfig.createColumnConfig(ctx.getConfiguration(), "LOGGER", "%logger", null, null, null, null),
            ColumnConfig.createColumnConfig(ctx.getConfiguration(), "MESSAGE", "%message", null, null, null, null),
            ColumnConfig.createColumnConfig(ctx.getConfiguration(), "THROWABLE", "%ex{full}", null, null, null, null)
        };
        for (Map.Entry<String, DataSource> entry : domainsHolder.getDomains().entrySet()) {
            Appender appender = ctx.getConfiguration().getAppender("audit_for_" + entry.getKey());
            if (appender == null) {
                appender = JdbcAppender.createAppender(
                        "audit_for_" + entry.getKey(),
                        "false",
                        null,
                        new DataSourceConnectionSource(entry.getValue()),
                        "0",
                        "SYNCOPEAUDIT",
                        columns);
                appender.start();
                ctx.getConfiguration().addAppender(appender);
            }

            LoggerConfig logConf = new LoggerConfig(
                    AuditManager.getDomainAuditLoggerName(entry.getKey()), null, false);
            logConf.addAppender(appender, Level.DEBUG, null);
            ctx.getConfiguration().addLogger(AuditManager.getDomainAuditLoggerName(entry.getKey()), logConf);
        }
        ctx.updateLoggers();

        // 2. Aligning log4j conf with database content
        Map<String, Logger> syncopeLoggers = new HashMap<>();
        for (Logger syncopeLogger : loggerDAO.findAll(LoggerType.LOG)) {
            syncopeLoggers.put(syncopeLogger.getKey(), syncopeLogger);
        }

        for (Logger syncopeLogger : loggerDAO.findAll(LoggerType.AUDIT)) {
            syncopeLoggers.put(syncopeLogger.getKey(), syncopeLogger);
        }

        /*
         * Traverse all defined log4j loggers: if there is a matching SyncopeLogger, set log4j level accordingly,
         * otherwise create a SyncopeLogger instance with given name and level.
         */
        for (LoggerConfig logConf : ctx.getConfiguration().getLoggers().values()) {
            final String loggerName = LogManager.ROOT_LOGGER_NAME.equals(logConf.getName())
                    ? SyncopeConstants.ROOT_LOGGER : logConf.getName();
            if (logConf.getLevel() != null) {
                if (syncopeLoggers.containsKey(loggerName)) {
                    logConf.setLevel(syncopeLoggers.get(loggerName).getLevel().getLevel());
                    syncopeLoggers.remove(loggerName);
                } else if (!loggerName.equals(LoggerType.AUDIT.getPrefix())) {
                    Logger syncopeLogger = entityFactory.newEntity(Logger.class);
                    syncopeLogger.setKey(loggerName);
                    syncopeLogger.setLevel(LoggerLevel.fromLevel(logConf.getLevel()));
                    syncopeLogger.setType(loggerName.startsWith(LoggerType.AUDIT.getPrefix())
                            ? LoggerType.AUDIT
                            : LoggerType.LOG);
                    loggerDAO.save(syncopeLogger);
                }
            }
        }

        /*
         * Foreach SyncopeLogger not found in log4j create a new log4j logger with given name and level.
         */
        for (Logger syncopeLogger : syncopeLoggers.values()) {
            LoggerConfig logConf = ctx.getConfiguration().getLoggerConfig(syncopeLogger.getKey());
            logConf.setLevel(syncopeLogger.getLevel().getLevel());
        }

        ctx.updateLoggers();
    }

    private static class DataSourceConnectionSource implements ConnectionSource {

        private final DataSource dataSource;

        public DataSourceConnectionSource(final DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DataSourceUtils.getConnection(dataSource);
        }

    }
}
