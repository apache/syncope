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
package org.syncope.core.util.multiparent;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class MultiParentNode<T> {

    private final T object;

    private Set<MultiParentNode<T>> parents;

    private Set<MultiParentNode<T>> children;

    public MultiParentNode(final T object) {
        this.object = object;
        parents = new HashSet<MultiParentNode<T>>();
        children = new HashSet<MultiParentNode<T>>();
    }

    public boolean isRoot() {
        return parents.isEmpty();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public T getObject() {
        return object;
    }

    public boolean isParent(final MultiParentNode<T> child) {
        return children.contains(child) && child.isChild(this);
    }

    public boolean isChild(final MultiParentNode<T> parent) {
        return parents.contains(parent) && parent.isParent(this);
    }

    public Set<MultiParentNode<T>> getChildren() {
        return children;
    }

    public Set<MultiParentNode<T>> getParents() {
        return parents;
    }

    public void addParent(final MultiParentNode<T> parent) {
        if (parent != null) {
            parents.add(parent);
            parent.children.add(this);
        }
    }

    public void removeParent(final MultiParentNode<T> parent) {
        if (parent != null) {
            parents.remove(parent);
            parent.children.remove(this);
        }
    }

    public void addChild(final MultiParentNode<T> child)
            throws CycleInMultiParentTreeException {

        if (child != null) {
            if (MultiParentNodeOp.findInTree(child, getObject()) != null) {
                throw new CycleInMultiParentTreeException(
                        "This node is descendant of given child node");
            }

            children.add(child);
            child.parents.add(this);
        }
    }

    public void removeChild(final MultiParentNode<T> child) {
        if (child != null) {
            children.remove(child);
            child.parents.remove(this);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
