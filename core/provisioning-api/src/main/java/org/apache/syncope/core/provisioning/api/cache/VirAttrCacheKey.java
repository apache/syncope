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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Cache entry key.
 */
@SuppressWarnings("squid:S2065")
public class VirAttrCacheKey {

    /**
     * Any type name.
     */
    private final transient String anyType;

    /**
     * Any object key.
     */
    private final transient String any;

    /**
     * Virtual attribute schema name.
     */
    private final transient String schema;

    public VirAttrCacheKey(final String anyType, final String any, final String schema) {
        this.anyType = anyType;
        this.any = any;
        this.schema = schema;
    }

    public String getAnyType() {
        return anyType;
    }

    public String getAny() {
        return any;
    }

    public String getSchema() {
        return schema;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(anyType).
                append(any).
                append(schema).
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
        final VirAttrCacheKey other = (VirAttrCacheKey) obj;
        return new EqualsBuilder().
                append(anyType, other.anyType).
                append(any, other.any).
                append(schema, other.schema).
                build();
    }

    @Override
    public String toString() {
        return "VirAttrCacheKey{" + "anyType=" + anyType + ", any=" + any + ", schema=" + schema + '}';
    }
}
