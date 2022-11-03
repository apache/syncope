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
import java.util.Optional;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.db.ColumnMapping;
import org.apache.logging.log4j.core.appender.db.jdbc.AbstractConnectionSource;
import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class JdbcAuditAppender extends DefaultAuditAppender {

    public JdbcAuditAppender(final String domain, final DataSource domainDataSource) {
        super(domain);

        LoggerContext logCtx = (LoggerContext) LogManager.getContext(false);

        ColumnMapping[] columnMappings = {
            ColumnMapping.newBuilder().
            setConfiguration(logCtx.getConfiguration()).setName("EVENT_DATE").setType(Timestamp.class).build(),
            ColumnMapping.newBuilder().
            setConfiguration(logCtx.getConfiguration()).setName("LOGGER_LEVEL").setPattern("%level").build(),
            ColumnMapping.newBuilder().
            setConfiguration(logCtx.getConfiguration()).setName("LOGGER").setPattern("%logger").build(),
            ColumnMapping.newBuilder().
            setConfiguration(logCtx.getConfiguration()).
            setName(AuditConfDAO.AUDIT_ENTRY_MESSAGE_COLUMN).setPattern("%message").build(),
            ColumnMapping.newBuilder().
            setConfiguration(logCtx.getConfiguration()).setName("THROWABLE").setPattern("%ex{full}").build()
        };

        targetAppender = Optional.ofNullable(logCtx.getConfiguration().<Appender>getAppender(getTargetAppenderName())).
                orElseGet(() -> {
                    JdbcAppender a = JdbcAppender.newBuilder().
                            setName(getTargetAppenderName()).
                            setIgnoreExceptions(false).
                            setConnectionSource(new DataSourceConnectionSource(domain, domainDataSource)).
                            setBufferSize(0).
                            setTableName(AuditConfDAO.AUDIT_ENTRY_TABLE).
                            setColumnMappings(columnMappings).
                            build();
                    a.start();
                    logCtx.getConfiguration().addAppender(a);
                    return a;
                });
    }

    @Override
    public String getTargetAppenderName() {
        return "audit_for_" + domain;
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
