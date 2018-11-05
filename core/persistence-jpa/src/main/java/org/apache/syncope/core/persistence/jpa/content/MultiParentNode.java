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
package org.apache.syncope.core.persistence.jpa.content;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

class MultiParentNode<T> {

    private final T object;

    private final Set<MultiParentNode<T>> children = new HashSet<>();

    private int level;

    private boolean exploited = false;

    MultiParentNode(final T object) {
        this.object = object;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(final int level) {
        this.level = level;
    }

    boolean isExploited() {
        return exploited;
    }

    void setExploited(final boolean exploited) {
        this.exploited = exploited;
    }

    public T getObject() {
        return object;
    }

    public boolean isParent(final MultiParentNode<T> child) {
        return children.contains(child);
    }

    public boolean isChild(final MultiParentNode<T> parent) {
        return parent.isParent(this);
    }

    public Set<MultiParentNode<T>> getChildren() {
        return children;
    }

    public void addParent(final MultiParentNode<T> parent) {
        if (parent != null) {
            parent.children.add(this);
        }
    }

    public void removeParent(final MultiParentNode<T> parent) {
        if (parent != null) {
            parent.children.remove(this);
        }
    }

    public void addChild(final MultiParentNode<T> child) {
        if (child != null) {
            children.add(child);
        }
    }

    public void removeChild(final MultiParentNode<T> child) {
        if (child != null) {
            children.remove(child);
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(object).
                append(children).
                append(level).
                append(exploited).
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
        @SuppressWarnings("unchecked")
        final MultiParentNode<T> other = (MultiParentNode<T>) obj;
        return new EqualsBuilder().
                append(object, other.object).
                append(children, other.children).
                append(level, other.level).
                append(exploited, other.exploited).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(object).
                append(children).
                append(level).
                append(exploited).
                build();
    }
}
