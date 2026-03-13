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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class OIDCOpEntityTO implements EntityTO {

    private static final long serialVersionUID = 1285073386484048953L;

    private String key;

    private String jwks;

    private final Map<String, Set<String>> customScopes = new HashMap<>();

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getJWKS() {
        return jwks;
    }

    public void setJWKS(final String jwks) {
        this.jwks = jwks;
    }

    public Map<String, Set<String>> getCustomScopes() {
        return customScopes;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(key).
                append(jwks).
                append(customScopes).
                toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        OIDCOpEntityTO rhs = (OIDCOpEntityTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(this.key, rhs.key).
                append(this.jwks, rhs.jwks).
                append(this.customScopes, rhs.customScopes).
                isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(super.toString()).
                append("key", key).
                append("jwks", jwks).
                append("customScopes", customScopes).
                toString();
    }
}
