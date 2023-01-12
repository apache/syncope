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
package org.apache.syncope.core.spring.security;

import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.rest.api.RESTHeaders;

public class SyncopeAuthenticationDetails implements Serializable {

    private static final long serialVersionUID = -5899959397393502897L;

    private final String domain;

    private final String delegatedBy;

    public SyncopeAuthenticationDetails(final HttpServletRequest request) {
        this.domain = request.getHeader(RESTHeaders.DOMAIN);
        this.delegatedBy = request.getHeader(RESTHeaders.DELEGATED_BY);
    }

    public SyncopeAuthenticationDetails(final String domain, final String delegatedBy) {
        this.domain = domain;
        this.delegatedBy = delegatedBy;
    }

    public String getDomain() {
        return StringUtils.isBlank(domain)
                ? SyncopeConstants.MASTER_DOMAIN
                : domain;
    }

    public String getDelegatedBy() {
        return delegatedBy;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(domain).
                append(delegatedBy).
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
        final SyncopeAuthenticationDetails other = (SyncopeAuthenticationDetails) obj;
        return new EqualsBuilder().
                append(domain, other.domain).
                append(delegatedBy, other.delegatedBy).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(domain).
                append(delegatedBy).
                build();
    }
}
