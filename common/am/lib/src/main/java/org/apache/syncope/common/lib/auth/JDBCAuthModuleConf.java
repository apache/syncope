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
package org.apache.syncope.common.lib.auth;

import java.util.ArrayList;
import java.util.List;

public class JDBCAuthModuleConf implements AuthModuleConf {

    private static final long serialVersionUID = 8383233437907219385L;

    /**
     * SQL query to execute. Example: {@code SELECT * FROM table WHERE name=?}.
     */
    private String sql;

    /**
     * Password field/column name to retrieve.
     */
    private String fieldPassword = "password";

    /**
     * Boolean field that should indicate whether the account is expired.
     */
    private String fieldExpired;

    /**
     * Boolean field that should indicate whether the account is disabled.
     */
    private String fieldDisabled;

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
     * List of column names to fetch as user attributes.
     */
    private final List<String> principalAttributeList = new ArrayList<>();

    public String getSql() {
        return sql;
    }

    public void setSql(final String sql) {
        this.sql = sql;
    }

    public String getFieldPassword() {
        return fieldPassword;
    }

    public void setFieldPassword(final String fieldPassword) {
        this.fieldPassword = fieldPassword;
    }

    public String getFieldExpired() {
        return fieldExpired;
    }

    public void setFieldExpired(final String fieldExpired) {
        this.fieldExpired = fieldExpired;
    }

    public String getFieldDisabled() {
        return fieldDisabled;
    }

    public void setFieldDisabled(final String fieldDisabled) {
        this.fieldDisabled = fieldDisabled;
    }

    public List<String> getPrincipalAttributeList() {
        return principalAttributeList;
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
}
