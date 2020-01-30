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

import java.util.Objects;

/**
 * Cache entry key.
 */
@SuppressWarnings("squid:S2065")
public class VirAttrCacheKey {

    /**
     * Any type name.
     */
    private final String type;

    /**
     * Any object key.
     */
    private final transient String key;

    /**
     * Virtual attribute schema name.
     */
    private final transient String virSchema;

    public VirAttrCacheKey(final String type, final String key, final String virSchema) {
        this.type = type;
        this.key = key;
        this.virSchema = virSchema;
    }

    public String getKind() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getVirSchema() {
        return virSchema;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.type);
        hash = 89 * hash + Objects.hashCode(this.key);
        hash = 89 * hash + Objects.hashCode(this.virSchema);
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
        final VirAttrCacheKey other = (VirAttrCacheKey) obj;
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.key, other.key)) {
            return false;
        }
        return Objects.equals(this.virSchema, other.virSchema);
    }

    @Override
    public String toString() {
        return "VirAttrCacheKey{" + "type=" + type + ", key=" + key + ", virSchema=" + virSchema + '}';
    }
}
