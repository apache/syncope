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

/**
 * Ccahe entry key.
 */
public class VirAttrCacheKey {

    /**
     * User ID.
     */
    private final transient Long userId;

    /**
     * Virtual attribute schema name.
     */
    private final transient String virAttrSchema;

    public VirAttrCacheKey(final Long userId, final String virAttrSchema) {
        this.userId = userId;
        this.virAttrSchema = virAttrSchema;
    }

    public Long getUserId() {
        return userId;
    }

    public String getVirAttrSchema() {
        return virAttrSchema;
    }

    @Override
    public int hashCode() {
        return 31 * (31
                + (userId == null ? 0 : userId.hashCode()))
                + virAttrSchema == null ? 0 : virAttrSchema.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof VirAttrCacheKey
                && ((userId == null && ((VirAttrCacheKey) o).getUserId() == null)
                || userId.equals(((VirAttrCacheKey) o).getUserId()))
                && ((virAttrSchema == null && ((VirAttrCacheKey) o).getVirAttrSchema() == null)
                || virAttrSchema.equals(((VirAttrCacheKey) o).getVirAttrSchema()));
    }
}
