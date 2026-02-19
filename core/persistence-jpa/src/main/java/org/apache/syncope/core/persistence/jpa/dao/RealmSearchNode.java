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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class RealmSearchNode {

    public enum Type {
        AND,
        OR,
        LEAF
    }

    public static class Leaf extends RealmSearchNode {

        private final String clause;

        protected Leaf(final String clause) {
            super(Type.LEAF);
            this.clause = clause;
        }

        public String getClause() {
            return clause;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().
                    appendSuper(super.hashCode()).
                    append(clause).
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
            final Leaf other = (Leaf) obj;

            return new EqualsBuilder().
                    appendSuper(super.equals(obj)).
                    append(clause, other.clause).
                    build();
        }

        @Override
        public String toString() {
            return "LeafNode{clause=" + clause + '}';
        }
    }

    private final Type type;

    private final List<RealmSearchNode> children = new ArrayList<>();

    public RealmSearchNode(final Type type) {
        this.type = type;
    }

    protected Type getType() {
        return type;
    }

    protected boolean add(final RealmSearchNode child) {
        if (type == Type.LEAF) {
            throw new IllegalArgumentException("Cannot add children to a leaf node");
        }
        return children.add(child);
    }

    protected List<RealmSearchNode> getChildren() {
        return children;
    }

    protected Optional<Leaf> asLeaf() {
        return type == Type.LEAF
                ? Optional.of((Leaf) this)
                : Optional.empty();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(type).
                append(children).
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
        final RealmSearchNode other = (RealmSearchNode) obj;

        return new EqualsBuilder().
                append(type, other.type).
                append(children, other.children).
                build();
    }

    @Override
    public String toString() {
        return "Node{" + "type=" + type + ", children=" + children + '}';
    }
}
