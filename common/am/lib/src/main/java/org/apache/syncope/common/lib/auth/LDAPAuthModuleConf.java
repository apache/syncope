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

public class LDAPAuthModuleConf implements AuthModuleConf {

    private static final long serialVersionUID = -471527731042579422L;

    protected String searchFilter;

    /**
     * The attribute value that should be used
     * for the authenticated username, upon a successful authentication
     * attempt.
     */
    private String userIdAttribute;

    /**
     * Whether subtree searching is allowed.
     */
    private boolean subtreeSearch = true;

    private String ldapUrl;

    /**
     * The bind DN to use when connecting to LDAP.
     * LDAP connection configuration injected into the LDAP connection pool
     * can be initialized with the following parameters:
     * <ul>
     * <li>{@code bindDn/bindCredential} provided - Use the provided credentials
     * to bind when initializing connections.</li>
     * <li>{@code bindDn/bindCredential} set to {@code *} - Use a fast-bind
     * strategy to initialize the pool.</li>
     * <li>{@code bindDn/bindCredential} set to blank - Skip connection
     * initializing; perform operations anonymously.</li>
     * <li>SASL mechanism provided - Use the given SASL mechanism
     * to bind when initializing connections. </li>
     * </ul>
     */
    private String bindDn;

    /**
     * The bind credential to use when connecting to LDAP.
     */
    private String bindCredential;

    private String baseDn;

    /**
     * List of attribute names to fetch as user attributes.
     */
    private final List<String> principalAttributeList = new ArrayList<>();

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(final String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public String getUserIdAttribute() {
        return userIdAttribute;
    }

    public void setUserIdAttribute(final String userIdAttribute) {
        this.userIdAttribute = userIdAttribute;
    }

    public boolean isSubtreeSearch() {
        return subtreeSearch;
    }

    public void setSubtreeSearch(final boolean subtreeSearch) {
        this.subtreeSearch = subtreeSearch;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(final String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getBindDn() {
        return bindDn;
    }

    public void setBindDn(final String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindCredential() {
        return bindCredential;
    }

    public void setBindCredential(final String bindCredential) {
        this.bindCredential = bindCredential;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(final String baseDn) {
        this.baseDn = baseDn;
    }

    public List<String> getPrincipalAttributeList() {
        return principalAttributeList;
    }
}
