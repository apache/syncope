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
package org.apache.syncope.common.lib.attr;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AttrRepoTO;

public class SyncopeAttrRepoConf implements AttrRepoConf {

    private static final long serialVersionUID = -3334329948161152222L;

    private String domain = SyncopeConstants.MASTER_DOMAIN;

    /**
     * User FIQL filter to use for searching.
     */
    protected String searchFilter;

    /**
     * Specify the username for REST authentication.
     */
    private String basicAuthUsername;

    /**
     * Specify the password for REST authentication.
     */
    private String basicAuthPassword;

    /**
     * Headers, defined as a Map, to include in the request when making the REST call.
     * Will overwrite any header that CAS is pre-defined to
     * send and include in the request. Key in the map should be the header name
     * and the value in the map should be the header value.
     */
    private final Map<String, String> headers = new HashMap<>();

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(final String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    public void setBasicAuthUsername(final String basicAuthUsername) {
        this.basicAuthUsername = basicAuthUsername;
    }

    public String getBasicAuthPassword() {
        return basicAuthPassword;
    }

    public void setBasicAuthPassword(final String basicAuthPassword) {
        this.basicAuthPassword = basicAuthPassword;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public Map<String, Object> map(final AttrRepoTO attrRepo, final Mapper mapper) {
        return mapper.map(attrRepo, this);
    }
}
