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
package org.apache.syncope.core.persistence.common.content;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MultiParentNode {

    private final String object;

    private final Set<MultiParentNode> children = new HashSet<>();

    private boolean exploited = false;

    public MultiParentNode(final String object) {
        this.object = object;
    }

    public String getObject() {
        return object;
    }

    public boolean isParent(final MultiParentNode child) {
        return children.contains(child);
    }

    public boolean isChild(final MultiParentNode parent) {
        return parent.isParent(this);
    }

    public Set<MultiParentNode> getChildren() {
        return children;
    }

    public void addParent(final MultiParentNode parent) {
        if (parent != null) {
            parent.children.add(this);
        }
    }

    public void removeParent(final MultiParentNode parent) {
        if (parent != null) {
            parent.children.remove(this);
        }
    }

    public void addChild(final MultiParentNode child) {
        if (child != null) {
            children.add(child);
        }
    }

    public void removeChild(final MultiParentNode child) {
        if (child != null) {
            children.remove(child);
        }
    }

    public boolean isExploited() {
        return exploited;
    }

    public void setExploited(final boolean exploited) {
        this.exploited = exploited;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(object).
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
        final MultiParentNode other = (MultiParentNode) obj;
        return new EqualsBuilder().
                append(object, other.object).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(object).
                build();
    }
}
