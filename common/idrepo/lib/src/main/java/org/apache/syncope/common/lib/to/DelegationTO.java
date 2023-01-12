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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.PathParam;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Schema(allOf = { AbstractStartEndBean.class })
public class DelegationTO extends AbstractStartEndBean implements EntityTO {

    private static final long serialVersionUID = 18031949556054L;

    private String key;

    private String delegating;

    private String delegated;

    private final Set<String> roles = new TreeSet<>();

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getDelegating() {
        return delegating;
    }

    public void setDelegating(final String delegating) {
        this.delegating = delegating;
    }

    public String getDelegated() {
        return delegated;
    }

    public void setDelegated(final String delegated) {
        this.delegated = delegated;
    }

    @JacksonXmlElementWrapper(localName = "roles")
    @JacksonXmlProperty(localName = "role")
    public Set<String> getRoles() {
        return roles;
    }

    @Schema(accessMode = Schema.AccessMode.READ_WRITE)
    @Override
    public OffsetDateTime getStart() {
        return super.getStart();
    }

    @Schema(accessMode = Schema.AccessMode.READ_WRITE)
    @Override
    public OffsetDateTime getEnd() {
        return super.getEnd();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(key).
                append(delegating).
                append(delegated).
                append(roles).
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
        final DelegationTO other = (DelegationTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(key, other.key).
                append(delegating, other.delegating).
                append(delegated, other.delegated).
                append(roles, other.roles).
                build();
    }
}
