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
package org.apache.syncope.core.util;

import org.apache.syncope.types.AttributableType;

/**
 * Ccahe entry key.
 */
public class VirAttrCacheKey {

    /**
     * Subject type.
     */
    private final AttributableType type;

    /**
     * Subject ID.
     */
    private final transient Long id;

    /**
     * Virtual attribute schema name.
     */
    private final transient String virAttrSchema;

    public VirAttrCacheKey(final AttributableType type, final Long id, final String virAttrSchema) {
        this.type = type;
        this.id = id;
        this.virAttrSchema = virAttrSchema;
    }

    public Long getId() {
        return id;
    }

    public String getVirAttrSchema() {
        return virAttrSchema;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31
                + (id == null ? 0 : id.hashCode()))
                + (virAttrSchema == null ? 0 : virAttrSchema.hashCode()))
                + (type == null ? 0 : type.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof VirAttrCacheKey
                && ((id == null && ((VirAttrCacheKey) o).getId() == null)
                || id.equals(((VirAttrCacheKey) o).getId()))
                && ((virAttrSchema == null && ((VirAttrCacheKey) o).getVirAttrSchema() == null)
                || virAttrSchema.equals(((VirAttrCacheKey) o).getVirAttrSchema()));
    }
}
