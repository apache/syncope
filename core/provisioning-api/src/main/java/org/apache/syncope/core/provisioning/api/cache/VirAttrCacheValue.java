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
package org.apache.syncope.core.provisioning.api.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Cache entry value.
 */
public class VirAttrCacheValue {

    /**
     * Virtual attribute values.
     */
    private final List<String> values = new ArrayList<>();

    /**
     * Entry creation date.
     */
    private Date creationDate;

    /**
     * Entry access date.
     */
    private Date lastAccessDate;

    public VirAttrCacheValue(final Collection<Object> values) {
        creationDate = new Date();
        lastAccessDate = new Date();

        if (values != null) {
            values.forEach(value -> this.values.add(value.toString()));
        }
    }

    public List<String> getValues() {
        lastAccessDate = new Date();
        return values;
    }

    public Date getCreationDate() {
        return new Date(creationDate.getTime());
    }

    public void forceExpiring() {
        creationDate = new Date(0);
    }

    public Date getLastAccessDate() {
        return new Date(lastAccessDate.getTime());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(values).
                append(creationDate).
                append(lastAccessDate).
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
        final VirAttrCacheValue other = (VirAttrCacheValue) obj;
        return new EqualsBuilder().
                append(values, other.values).
                append(creationDate, other.creationDate).
                append(lastAccessDate, other.lastAccessDate).
                build();
    }

    @Override
    public String toString() {
        return "VirAttrCacheValue{"
                + "values=" + values + ", creationDate=" + creationDate + ", lastAccessDate=" + lastAccessDate
                + '}';
    }
}
