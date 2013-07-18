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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.types.AttributableType;

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
    private final transient String virSchema;

    public VirAttrCacheKey(final AttributableType type, final Long id, final String virSchema) {
        this.type = type;
        this.id = id;
        this.virSchema = virSchema;
    }

    public AttributableType getType() {
        return type;
    }

    public Long getId() {
        return id;
    }

    public String getVirSchema() {
        return virSchema;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, true);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, true);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE, true);
    }
}
