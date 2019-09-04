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
import java.util.Objects;

/**
 * Cache entry value.
 */
public class VirAttrCacheValue {

    /**
     * Virtual attribute values.
     */
    private final List<String> values;

    /**
     * Entry creation date.
     */
    private Date creationDate;

    /**
     * Entry access date.
     */
    private Date lastAccessDate;

    public VirAttrCacheValue() {
        this.creationDate = new Date();
        this.lastAccessDate = new Date();
        this.values = new ArrayList<>();
    }

    public void setValues(final Collection<Object> values) {
        this.values.clear();

        if (values != null) {
            values.forEach(value -> this.values.add(value.toString()));
        }
    }

    public Date getCreationDate() {
        if (creationDate != null) {
            return new Date(creationDate.getTime());
        }
        return null;
    }

    public void forceExpiring() {
        creationDate = new Date(0);
    }

    public List<String> getValues() {
        return values;
    }

    public Date getLastAccessDate() {
        if (lastAccessDate != null) {
            return new Date(lastAccessDate.getTime());
        }
        return null;
    }

    public void setLastAccessDate(final Date lastAccessDate) {
        if (lastAccessDate != null) {
            this.lastAccessDate = new Date(lastAccessDate.getTime());
        } else {
            this.lastAccessDate = null;
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.values);
        hash = 67 * hash + Objects.hashCode(this.creationDate);
        hash = 67 * hash + Objects.hashCode(this.lastAccessDate);
        return hash;
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
        if (!Objects.equals(this.values, other.values)) {
            return false;
        }
        if (!Objects.equals(this.creationDate, other.creationDate)) {
            return false;
        }
        return Objects.equals(this.lastAccessDate, other.lastAccessDate);
    }

    @Override
    public String toString() {
        return "VirAttrCacheValue{"
                + "values=" + values + ", creationDate=" + creationDate + ", lastAccessDate=" + lastAccessDate
                + '}';
    }
}
