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

import java.net.URI;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Neo4jDomain extends Domain {

    private static final long serialVersionUID = 9223353502929953472L;

    public static class Builder extends Domain.Builder<Neo4jDomain, Builder> {

        public Builder(final String key) {
            super(new Neo4jDomain(), key);
        }

        public Builder uri(final URI uri) {
            domain.uri = uri;
            return this;
        }

        public Builder username(final String username) {
            domain.username = username;
            return this;
        }

        public Builder password(final String password) {
            domain.password = password;
            return this;
        }

        public Builder maxConnectionPoolSize(final int maxConnectionPoolSize) {
            domain.maxConnectionPoolSize = maxConnectionPoolSize;
            return this;
        }
    }

    private URI uri;

    private String username;

    private String password;

    private int maxConnectionPoolSize = 100;

    @Override
    protected String defaultContentFile() {
        return "defaultContent.neo4j.xml";
    }

    public URI getUri() {
        return uri;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getMaxConnectionPoolSize() {
        return maxConnectionPoolSize;
    }

    public void setMaxConnectionPoolSize(final int maxConnectionPoolSize) {
        this.maxConnectionPoolSize = maxConnectionPoolSize;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(uri).
                append(username).
                append(password).
                append(maxConnectionPoolSize).
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
        final Neo4jDomain other = (Neo4jDomain) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(uri, other.uri).
                append(username, other.username).
                append(password, other.password).
                append(maxConnectionPoolSize, other.maxConnectionPoolSize).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                appendSuper(super.toString()).
                append(uri).
                append(username).
                append(password).
                append(maxConnectionPoolSize).
                build();
    }
}
