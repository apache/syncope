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
package org.apache.syncope.core.persistence.api.dao.search;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class SearchCond extends AbstractSearchCond {

    private static final long serialVersionUID = 661560782247499526L;

    public enum Type {

        LEAF,
        NOT_LEAF,
        AND,
        OR

    }

    private Type type;

    private AbstractSearchCond leaf;

    private SearchCond left;

    private SearchCond right;

    public static SearchCond getLeaf(final AbstractSearchCond leaf) {
        SearchCond cond;
        if (leaf instanceof SearchCond) {
            cond = (SearchCond) leaf;
        } else {
            cond = new SearchCond();
            cond.leaf = leaf;
        }

        cond.type = Type.LEAF;

        return cond;
    }

    public static SearchCond getNotLeaf(final AbstractSearchCond leaf) {
        SearchCond cond = getLeaf(leaf);

        cond.type = Type.NOT_LEAF;

        return cond;
    }

    public static SearchCond getAnd(final SearchCond left, final SearchCond right) {
        SearchCond cond = new SearchCond();

        cond.type = Type.AND;
        cond.left = left;
        cond.right = right;

        return cond;
    }

    public static SearchCond getAnd(final List<SearchCond> conditions) {
        if (conditions.size() == 1) {
            return conditions.get(0);
        } else if (conditions.size() > 2) {
            SearchCond removed = conditions.remove(0);
            return getAnd(removed, getAnd(conditions));
        } else {
            return getAnd(conditions.get(0), conditions.get(1));
        }
    }

    public static SearchCond getOr(final SearchCond left, final SearchCond right) {
        SearchCond cond = new SearchCond();

        cond.type = Type.OR;
        cond.left = left;
        cond.right = right;

        return cond;
    }

    public static SearchCond getOr(final List<SearchCond> conditions) {
        if (conditions.size() == 1) {
            return conditions.get(0);
        } else if (conditions.size() > 2) {
            SearchCond removed = conditions.remove(0);
            return getOr(removed, getOr(conditions));
        } else {
            return getOr(conditions.get(0), conditions.get(1));
        }
    }

    public Optional<AnyTypeCond> getAnyTypeCond() {
        return Optional.ofNullable(leaf instanceof AnyTypeCond ? (AnyTypeCond) leaf : null);
    }

    /**
     * Not a simple getter: recursively scans the search condition tree.
     *
     * @return the AnyType key or {@code NULL} if no type condition was found
     */
    public String hasAnyTypeCond() {
        String anyTypeName = null;

        if (type == null) {
            return null;
        }

        switch (type) {
            case LEAF:
            case NOT_LEAF:
                if (leaf instanceof AnyTypeCond) {
                    anyTypeName = ((AnyTypeCond) leaf).getAnyTypeKey();
                }
                break;

            case AND:
            case OR:
                if (left != null) {
                    anyTypeName = left.hasAnyTypeCond();
                }
                if (anyTypeName == null && right != null) {
                    anyTypeName = right.hasAnyTypeCond();
                }
                break;

            default:
        }

        return anyTypeName;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractSearchCond> Optional<T> getLeaf(final Class<T> clazz) {
        return Optional.ofNullable((T) (clazz.isInstance(leaf) ? leaf : null));
    }

    public SearchCond getLeft() {
        return left;
    }

    public SearchCond getRight() {
        return right;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean isValid() {
        boolean isValid = false;

        if (type == null) {
            return false;
        }

        switch (type) {
            case LEAF:
            case NOT_LEAF:
                isValid = leaf != null && leaf.isValid();
                break;

            case AND:
            case OR:
                isValid = left != null && right != null
                        && left.isValid() && right.isValid();
                break;

            default:
        }

        return isValid;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(type).
                append(leaf).
                append(left).
                append(right).
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
        final SearchCond other = (SearchCond) obj;
        return new EqualsBuilder().
                append(type, other.type).
                append(leaf, other.leaf).
                append(left, other.left).
                append(right, other.right).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(type).
                append(leaf).
                append(left).
                append(right).
                build();
    }
}
