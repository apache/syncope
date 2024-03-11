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
package org.apache.syncope.common.keymaster.client.api.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class JPADomain extends Domain {

    private static final long serialVersionUID = 18711766451769410L;

    public enum TransactionIsolation {
        TRANSACTION_NONE,
        TRANSACTION_READ_COMMITTED,
        TRANSACTION_READ_UNCOMMITTED,
        TRANSACTION_REPEATABLE_READ,
        TRANSACTION_SERIALIZABLE

    }

    public static class Builder extends Domain.Builder<JPADomain, Builder> {

        public Builder(final String key) {
            super(new JPADomain(), key);
        }

        public Builder jdbcDriver(final String jdbcDriver) {
            this.domain.jdbcDriver = jdbcDriver;
            return this;
        }

        public Builder jdbcURL(final String jdbcURL) {
            this.domain.jdbcURL = jdbcURL;
            return this;
        }

        public Builder dbSchema(final String dbSchema) {
            if (StringUtils.isNotBlank(dbSchema)) {
                this.domain.dbSchema = dbSchema;
            }
            return this;
        }

        public Builder dbUsername(final String dbUsername) {
            this.domain.dbUsername = dbUsername;
            return this;
        }

        public Builder dbPassword(final String dbPassword) {
            this.domain.dbPassword = dbPassword;
            return this;
        }

        public Builder transactionIsolation(final TransactionIsolation transactionIsolation) {
            this.domain.transactionIsolation = transactionIsolation;
            return this;
        }

        public Builder poolMaxActive(final int poolMaxActive) {
            this.domain.poolMaxActive = poolMaxActive;
            return this;
        }

        public Builder poolMinIdle(final int poolMinIdle) {
            this.domain.poolMinIdle = poolMinIdle;
            return this;
        }

        public Builder orm(final String orm) {
            this.domain.orm = orm;
            return this;
        }

        public Builder databasePlatform(final String databasePlatform) {
            this.domain.databasePlatform = databasePlatform;
            return this;
        }
    }

    private String jdbcDriver;

    private String jdbcURL;

    private String dbSchema;

    private String dbUsername;

    private String dbPassword;

    private TransactionIsolation transactionIsolation = TransactionIsolation.TRANSACTION_READ_COMMITTED;

    private int poolMaxActive = 10;

    private int poolMinIdle = 2;

    private String orm = "META-INF/spring-orm.xml";

    private String databasePlatform;

    @Override
    protected String defaultContentFile() {
        return "defaultContent.jpa.xml";
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public String getJdbcURL() {
        return jdbcURL;
    }

    public String getDbSchema() {
        return dbSchema;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public TransactionIsolation getTransactionIsolation() {
        return transactionIsolation;
    }

    public int getPoolMaxActive() {
        return poolMaxActive;
    }

    public void setPoolMaxActive(final int poolMaxActive) {
        this.poolMaxActive = poolMaxActive;
    }

    public int getPoolMinIdle() {
        return poolMinIdle;
    }

    public void setPoolMinIdle(final int poolMinIdle) {
        this.poolMinIdle = poolMinIdle;
    }

    public String getOrm() {
        return orm;
    }

    public String getDatabasePlatform() {
        return databasePlatform;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(jdbcDriver).
                append(jdbcURL).
                append(dbSchema).
                append(dbUsername).
                append(dbPassword).
                append(transactionIsolation).
                append(poolMaxActive).
                append(poolMinIdle).
                append(orm).
                append(databasePlatform).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JPADomain other = (JPADomain) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(jdbcDriver, other.jdbcDriver).
                append(jdbcURL, other.jdbcURL).
                append(dbSchema, other.dbSchema).
                append(dbUsername, other.dbUsername).
                append(dbPassword, other.dbPassword).
                append(transactionIsolation, other.transactionIsolation).
                append(poolMaxActive, other.poolMaxActive).
                append(poolMinIdle, other.poolMinIdle).
                append(orm, other.orm).
                append(databasePlatform, other.databasePlatform).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                appendSuper(super.toString()).
                append(jdbcDriver).
                append(jdbcURL).
                append(dbSchema).
                append(dbUsername).
                append(dbPassword).
                append(transactionIsolation).
                append(poolMaxActive).
                append(poolMinIdle).
                append(orm).
                append(databasePlatform).
                build();
    }
}
