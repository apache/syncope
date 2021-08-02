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
package org.apache.syncope.core.persistence.jpa;

import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.types.CipherAlgorithm;

public class DomainProperties {

    private String key;

    private String jdbcDriver;

    private String jdbcURL;

    private String dbSchema;

    private String dbUsername;

    private String dbPassword;

    private Domain.TransactionIsolation transactionIsolation = Domain.TransactionIsolation.TRANSACTION_READ_COMMITTED;

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

    public void setKey(final String key) {
        this.key = key;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(final String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public String getJdbcURL() {
        return jdbcURL;
    }

    public void setJdbcURL(final String jdbcURL) {
        this.jdbcURL = jdbcURL;
    }

    public String getDbSchema() {
        return dbSchema;
    }

    public void setDbSchema(final String dbSchema) {
        this.dbSchema = dbSchema;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(final String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(final String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public Domain.TransactionIsolation getTransactionIsolation() {
        return transactionIsolation;
    }

    public void setTransactionIsolation(final Domain.TransactionIsolation transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
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

    public void setAuditSql(final String auditSql) {
        this.auditSql = auditSql;
    }

    public String getOrm() {
        return orm;
    }

    public void setOrm(final String orm) {
        this.orm = orm;
    }

    public String getDatabasePlatform() {
        return databasePlatform;
    }

    public void setDatabasePlatform(final String databasePlatform) {
        this.databasePlatform = databasePlatform;
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

    public String getContent() {
        return content == null
                ? "classpath:domains/" + key + "Content.xml"
                : content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getKeymasterConfParams() {
        return keymasterConfParams == null
                ? "classpath:domains/" + key + "KeymasterConfParams.json"
                : keymasterConfParams;
    }

    public void setKeymasterConfParams(final String keymasterConfParams) {
        this.keymasterConfParams = keymasterConfParams;
    }
}
