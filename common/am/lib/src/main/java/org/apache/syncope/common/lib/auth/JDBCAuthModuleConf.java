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

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "jdbcAuthModuleConf")
@XmlType
public class JDBCAuthModuleConf extends AbstractAuthModuleConf {

    private static final long serialVersionUID = 8383233437907219385L;

    /**
     * SQL query to execute. Example: {@code SELECT * FROM table WHERE name=?}.
     */
    private String sql;

    /**
     * Password field/column name to retrieve.
     */
    private String fieldPassword;

    /**
     * Boolean field that should indicate whether the account is expired.
     */
    private String fieldExpired;

    /**
     * Boolean field that should indicate whether the account is disabled.
     */
    private String fieldDisabled;

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

    @XmlElementWrapper(name = "principalAttributeList")
    @XmlElement(name = "principalAttributeList")
    @JsonProperty("principalAttributeList")
    public List<String> getPrincipalAttributeList() {
        return principalAttributeList;
    }

}
