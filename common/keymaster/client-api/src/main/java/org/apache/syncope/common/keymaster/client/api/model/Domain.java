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

import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Domain implements Serializable {

    private static final long serialVersionUID = -5881851479361505961L;

    private static final Logger LOG = LoggerFactory.getLogger(Domain.class);

    public enum TransactionIsolation {
        TRANSACTION_NONE,
        TRANSACTION_READ_COMMITTED,
        TRANSACTION_READ_UNCOMMITTED,
        TRANSACTION_REPEATABLE_READ,
        TRANSACTION_SERIALIZABLE

    }

    public static class Builder {

        private final Domain domain;

        public Builder(final String key) {
            this.domain = new Domain();
            this.domain.key = key;
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

        public Builder auditSql(final String auditSql) {
            this.domain.auditSql = auditSql;
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

        public Builder adminPassword(final String adminPassword) {
            this.domain.adminPassword = adminPassword;
            return this;
        }

        public Builder adminCipherAlgorithm(final CipherAlgorithm adminCipherAlgorithm) {
            this.domain.adminCipherAlgorithm = adminCipherAlgorithm;
            return this;
        }

        public Builder content(final String content) {
            this.domain.content = content;
            return this;
        }

        public Builder keymasterConfParams(final String keymasterConfParams) {
            this.domain.keymasterConfParams = keymasterConfParams;
            return this;
        }

        public Domain build() {
            return this.domain;
        }
    }

    private String key;

    private String jdbcDriver;

    private String jdbcURL;

    private String dbSchema;

    private String dbUsername;

    private String dbPassword;

    private TransactionIsolation transactionIsolation = TransactionIsolation.TRANSACTION_READ_COMMITTED;

    private int poolMaxActive = 10;

    private int poolMinIdle = 2;

    private String auditSql = "audit.sql";

    private String orm = "META-INF/spring-orm.xml";

    private String databasePlatform;

    private String adminPassword;

    private CipherAlgorithm adminCipherAlgorithm = CipherAlgorithm.SHA512;

    private String content;

    private String keymasterConfParams;

    public String getKey() {
        return key;
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

    public String getAuditSql() {
        return auditSql;
    }

    public String getOrm() {
        return orm;
    }

    public String getDatabasePlatform() {
        return databasePlatform;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(final String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public CipherAlgorithm getAdminCipherAlgorithm() {
        return adminCipherAlgorithm;
    }

    public void setAdminCipherAlgorithm(final CipherAlgorithm adminCipherAlgorithm) {
        this.adminCipherAlgorithm = adminCipherAlgorithm;
    }

    private String read(final String filename) {
        String read = null;
        try {
            read = IOUtils.toString(Domain.class.getResourceAsStream('/' + filename));
        } catch (IOException e) {
            LOG.error("Could not read {}", filename, e);
        }

        return read;
    }

    public String getContent() {
        if (content == null) {
            content = read("defaultContent.xml");
        }

        return content;
    }

    public String getKeymasterConfParams() {
        if (keymasterConfParams == null) {
            keymasterConfParams = read("defaultKeymasterConfParams.json");
        }

        return keymasterConfParams;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(jdbcDriver).
                append(jdbcURL).
                append(dbSchema).
                append(dbUsername).
                append(dbPassword).
                append(transactionIsolation).
                append(poolMaxActive).
                append(poolMinIdle).
                append(auditSql).
                append(orm).
                append(databasePlatform).
                append(adminPassword).
                append(adminCipherAlgorithm).
                append(content).
                append(keymasterConfParams).
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
        final Domain other = (Domain) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(jdbcDriver, other.jdbcDriver).
                append(jdbcURL, other.jdbcURL).
                append(dbSchema, other.dbSchema).
                append(dbUsername, other.dbUsername).
                append(dbPassword, other.dbPassword).
                append(transactionIsolation, other.transactionIsolation).
                append(poolMaxActive, other.poolMaxActive).
                append(poolMinIdle, other.poolMinIdle).
                append(auditSql, other.auditSql).
                append(orm, other.orm).
                append(databasePlatform, other.databasePlatform).
                append(adminPassword, other.adminPassword).
                append(adminCipherAlgorithm, other.adminCipherAlgorithm).
                append(content, other.content).
                append(keymasterConfParams, other.keymasterConfParams).
                build();
    }

    @Override
    public String toString() {
        return "Domain{"
                + "key=" + key
                + ", jdbcDriver=" + jdbcDriver
                + ", jdbcURL=" + jdbcURL
                + ", dbSchema=" + dbSchema
                + ", dbUsername=" + dbUsername
                + ", dbPassword=" + dbPassword
                + ", transactionIsolation=" + transactionIsolation
                + ", poolMaxSize=" + poolMaxActive
                + ", poolMinIdle=" + poolMinIdle
                + ", auditSql=" + auditSql
                + ", orm=" + orm
                + ", databasePlatform=" + databasePlatform
                + ", adminPassword=" + adminPassword
                + ", adminCipherAlgorithm=" + adminCipherAlgorithm
                + ", content=" + content
                + ", keymasterConfParams=" + keymasterConfParams
                + '}';
    }
}
