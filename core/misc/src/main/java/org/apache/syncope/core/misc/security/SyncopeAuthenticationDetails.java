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
package org.apache.syncope.core.misc.security;

import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.rest.api.RESTHeaders;

public class SyncopeAuthenticationDetails implements Serializable {

    private static final long serialVersionUID = -5899959397393502897L;

    private final String remoteAddress;

    private final String sessionId;

    private String domain;

    public SyncopeAuthenticationDetails(final HttpServletRequest request) {
        this.remoteAddress = request.getRemoteAddr();

        HttpSession session = request.getSession(false);
        this.sessionId = session == null ? null : session.getId();

        this.domain = request.getHeader(RESTHeaders.DOMAIN);
    }

    public SyncopeAuthenticationDetails(final String domain) {
        this.remoteAddress = null;
        this.sessionId = null;
        this.domain = domain;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
