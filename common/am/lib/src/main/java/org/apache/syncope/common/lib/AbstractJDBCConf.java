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
package org.apache.syncope.common.lib;

import java.io.Serializable;
import java.time.Duration;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractJDBCConf implements Serializable {

    private static final long serialVersionUID = 2675132530878660196L;

    /**
     * SQL query to execute. Example: {@code SELECT * FROM table WHERE name=?}.
     */
    private String sql;

    /**
     * The database dialect is a configuration setting for platform independent software (JPA, Hibernate, etc)
     * which allows such software to translate its generic SQL statements into vendor specific DDL, DML.
     */
    private String dialect = "org.hibernate.dialect.H2Dialect";

    /**
     * The JDBC driver used to connect to the database.
     */
    private String driverClass = "org.h2.Driver";

    /**
     * The database connection URL.
     */
    private String url = "jdbc:h2:tcp://localhost:9092/mem:authdb;DB_CLOSE_DELAY=-1";

    /**
     * The database user.
     * <p>
     * The database user must have sufficient permissions to be able to handle
     * schema changes and updates, when needed.
     */
    private String user = "sa";

    /**
     * The database connection password.
     */
    private String password = "sa";

    /**
     * Qualifies unqualified table names with the given catalog in generated SQL.
     */
    private String defaultCatalog;

    /**
     * Qualify unqualified table names with the given schema/tablespace in generated SQL.
     */
    private String defaultSchema;

    /**
     * The SQL query to be executed to test the validity of connections.
     * This is for "legacy" databases that do not support the JDBC4 {@code Connection.isValid()} API.
     */
    private String healthQuery = StringUtils.EMPTY;

    /**
     * Controls the maximum amount of time that a connection is allowed to sit idle in the pool.
     */
    private Duration idleTimeout = Duration.parse("PT10M");

    /**
     * Attempts to do a JNDI data source look up for the data source name specified.
     * Will attempt to locate the data source object as is.
     */
    private String dataSourceName;

    /**
     * Controls the minimum size that the pool is allowed
     * to reach, including both idle and in-use connections.
     */
    private int minPoolSize = 6;

    /**
     * Controls the maximum number of connections to keep
     * in the pool, including both idle and in-use connections.
     */
    private int maxPoolSize = 18;

    /**
     * Sets the maximum time in seconds that this data source will wait
     * while attempting to connect to a database.
     * A value of zero specifies that the timeout is the default system timeout
     * if there is one; otherwise, it specifies that there is no timeout.
     */
    private Duration maxPoolWait = Duration.parse("PT2S");

    /**
     * Whether or not pool suspension is allowed.
     * There is a performance impact when pool suspension is enabled.
     * Unless you need it (for a redundancy system for example) do not enable it.
     */
    private boolean poolSuspension;

    /**
     * The maximum number of milliseconds that the
     * pool will wait for a connection to be validated as alive.
     */
    private long poolTimeoutMillis = 1_000;

    /**
     * Controls the amount of time that a connection can be out of the pool before a message
     * is logged indicating a possible connection leak.
     */
    private Duration poolLeakThreshold = Duration.parse("PT6S");

    public String getSql() {
        return sql;
    }

    public void setSql(final String sql) {
        this.sql = sql;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(final String dialect) {
        this.dialect = dialect;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(final String driverClass) {
        this.driverClass = driverClass;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getDefaultCatalog() {
        return defaultCatalog;
    }

    public void setDefaultCatalog(final String defaultCatalog) {
        this.defaultCatalog = defaultCatalog;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(final String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    public String getHealthQuery() {
        return healthQuery;
    }

    public void setHealthQuery(final String healthQuery) {
        this.healthQuery = healthQuery;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(final Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(final String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(final int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(final int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Duration getMaxPoolWait() {
        return maxPoolWait;
    }

    public void setMaxPoolWait(final Duration maxPoolWait) {
        this.maxPoolWait = maxPoolWait;
    }

    public boolean isPoolSuspension() {
        return poolSuspension;
    }

    public void setPoolSuspension(final boolean poolSuspension) {
        this.poolSuspension = poolSuspension;
    }

    public long getPoolTimeoutMillis() {
        return poolTimeoutMillis;
    }

    public void setPoolTimeoutMillis(final long poolTimeoutMillis) {
        this.poolTimeoutMillis = poolTimeoutMillis;
    }

    public Duration getPoolLeakThreshold() {
        return poolLeakThreshold;
    }

    public void setPoolLeakThreshold(final Duration poolLeakThreshold) {
        this.poolLeakThreshold = poolLeakThreshold;
    }
}
