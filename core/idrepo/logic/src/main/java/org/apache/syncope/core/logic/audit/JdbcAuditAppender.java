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
package org.apache.syncope.core.logic.audit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.db.ColumnMapping;
import org.apache.logging.log4j.core.appender.db.jdbc.AbstractConnectionSource;
import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;

public class JdbcAuditAppender extends DefaultAuditAppender {

    @Autowired
    protected DomainHolder domainHolder;

    @Override
    protected void initTargetAppender() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

        ColumnMapping[] columnMappings = {
            ColumnMapping.newBuilder().
            setConfiguration(ctx.getConfiguration()).setName("EVENT_DATE").setType(Timestamp.class).build(),
            ColumnMapping.newBuilder().
            setConfiguration(ctx.getConfiguration()).setName("LOGGER_LEVEL").setPattern("%level").build(),
            ColumnMapping.newBuilder().
            setConfiguration(ctx.getConfiguration()).setName("LOGGER").setPattern("%logger").build(),
            ColumnMapping.newBuilder().
            setConfiguration(ctx.getConfiguration()).
            setName(AuditConfDAO.AUDIT_ENTRY_MESSAGE_COLUMN).setPattern("%message").build(),
            ColumnMapping.newBuilder().
            setConfiguration(ctx.getConfiguration()).setName("THROWABLE").setPattern("%ex{full}").build()
        };

        Appender appender = ctx.getConfiguration().getAppender("audit_for_" + domain);
        if (appender == null) {
            appender = JdbcAppender.newBuilder().
                    setName("audit_for_" + domain).
                    setIgnoreExceptions(false).
                    setConnectionSource(new DataSourceConnectionSource(domain, domainHolder.getDomains().get(domain))).
                    setBufferSize(0).
                    setTableName(AuditConfDAO.AUDIT_ENTRY_TABLE).
                    setColumnMappings(columnMappings).
                    build();
            appender.start();
            ctx.getConfiguration().addAppender(appender);
        }
        targetAppender = appender;
    }

    @Override
    public String getTargetAppenderName() {
        // not used
        return null;
    }

    protected static class DataSourceConnectionSource extends AbstractConnectionSource {

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
