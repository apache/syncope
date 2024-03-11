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
package org.apache.syncope.client.console.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.types.OpEvent;

public class EventCategory implements BaseBean {

    private static final long serialVersionUID = -4340060002701633401L;

    private OpEvent.CategoryType type;

    private String category;

    private String subcategory;

    private final List<String> ops = new ArrayList<>();

    /**
     * Constructor for Type.REST event category.
     */
    public EventCategory() {
        this(OpEvent.CategoryType.LOGIC);
    }

    /**
     * Constructor for the given Type event category.
     *
     * @param type event category type
     */
    public EventCategory(final OpEvent.CategoryType type) {
        super();
        this.type = type;
    }

    public OpEvent.CategoryType getType() {
        return type;
    }

    public void setType(final OpEvent.CategoryType type) {
        this.type = Optional.ofNullable(type).orElse(OpEvent.CategoryType.CUSTOM);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(final String subcategory) {
        this.subcategory = subcategory;
    }

    public List<String> getOps() {
        return ops;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(type).
                append(category).
                append(subcategory).
                append(ops).
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
        final EventCategory other = (EventCategory) obj;
        return new EqualsBuilder().
                append(type, other.type).
                append(category, other.category).
                append(subcategory, other.subcategory).
                append(ops, other.ops).
                build();
    }
}
