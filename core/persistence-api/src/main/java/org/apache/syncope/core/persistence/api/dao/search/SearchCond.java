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

public class SearchCond extends AbstractSearchCond {

    private static final long serialVersionUID = 661560782247499526L;

    public enum Type {

        LEAF,
        NOT_LEAF,
        AND,
        OR

    }

    private Type type;

    private AnyTypeCond anyTypeCond;

    private AnyCond anyCond;

    private AttributeCond attributeCond;

    private RelationshipCond relationshipCond;

    private RelationshipTypeCond relationshipTypeCond;

    private MembershipCond membershipCond;

    private RoleCond roleCond;

    private ResourceCond resourceCond;

    private AssignableCond assignableCond;

    private MemberCond memberCond;

    private SearchCond leftSearchCond;

    private SearchCond rightSearchCond;

    public static SearchCond getLeafCond(final AnyTypeCond anyTypeCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.LEAF;
        nodeCond.anyTypeCond = anyTypeCond;

        return nodeCond;
    }

    public static SearchCond getLeafCond(final AttributeCond attributeCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.LEAF;
        if (attributeCond instanceof AnyCond) {
            nodeCond.anyCond = (AnyCond) attributeCond;
        } else {
            nodeCond.attributeCond = attributeCond;
        }

        return nodeCond;
    }

    public static SearchCond getLeafCond(final RelationshipCond relationshipCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.LEAF;
        nodeCond.relationshipCond = relationshipCond;

        return nodeCond;
    }

    public static SearchCond getLeafCond(final RelationshipTypeCond relationshipTypeCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.LEAF;
        nodeCond.relationshipTypeCond = relationshipTypeCond;

        return nodeCond;
    }

    public static SearchCond getLeafCond(final MembershipCond membershipCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.LEAF;
        nodeCond.membershipCond = membershipCond;

        return nodeCond;
    }

    public static SearchCond getLeafCond(final RoleCond roleCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.LEAF;
        nodeCond.roleCond = roleCond;

        return nodeCond;
    }

    public static SearchCond getLeafCond(final ResourceCond resourceCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.LEAF;
        nodeCond.resourceCond = resourceCond;

        return nodeCond;
    }

    public static SearchCond getLeafCond(final AssignableCond assignableCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.LEAF;
        nodeCond.assignableCond = assignableCond;

        return nodeCond;
    }

    public static SearchCond getLeafCond(final MemberCond memberCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.LEAF;
        nodeCond.memberCond = memberCond;

        return nodeCond;
    }

    public static SearchCond getNotLeafCond(final AttributeCond attributeCond) {
        SearchCond nodeCond = getLeafCond(attributeCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static SearchCond getNotLeafCond(final RelationshipCond relationshipCond) {
        SearchCond nodeCond = getLeafCond(relationshipCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static SearchCond getNotLeafCond(final MembershipCond membershipCond) {
        SearchCond nodeCond = getLeafCond(membershipCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static SearchCond getNotLeafCond(final RoleCond roleCond) {
        SearchCond nodeCond = getLeafCond(roleCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static SearchCond getNotLeafCond(final ResourceCond resourceCond) {
        SearchCond nodeCond = getLeafCond(resourceCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static SearchCond getNotLeafCond(final MemberCond memberCond) {
        SearchCond nodeCond = getLeafCond(memberCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static SearchCond getNotLeafCond(final SearchCond nodeCond) {
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static SearchCond getAndCond(final SearchCond leftCond, final SearchCond rightCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.AND;
        nodeCond.leftSearchCond = leftCond;
        nodeCond.rightSearchCond = rightCond;

        return nodeCond;
    }

    public static SearchCond getAndCond(final List<SearchCond> conditions) {
        if (conditions.size() > 2) {
            SearchCond removed = conditions.remove(0);
            return getAndCond(removed, getAndCond(conditions));
        } else {
            return getAndCond(conditions.get(0), conditions.get(1));
        }
    }

    public static SearchCond getOrCond(final SearchCond leftCond, final SearchCond rightCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.OR;
        nodeCond.leftSearchCond = leftCond;
        nodeCond.rightSearchCond = rightCond;

        return nodeCond;
    }

    public static SearchCond getOrCond(final List<SearchCond> conditions) {
        if (conditions.size() > 2) {
            SearchCond removed = conditions.remove(0);
            return getOrCond(removed, getOrCond(conditions));
        } else {
            return getOrCond(conditions.get(0), conditions.get(1));
        }
    }

    public AnyTypeCond getAnyTypeCond() {
        return anyTypeCond;
    }

    public void setAnyTypeCond(final AnyTypeCond anyTypeCond) {
        this.anyTypeCond = anyTypeCond;
    }

    /**
     * Not a simple getter: recursively scans the search condition tree.
     *
     * @return the AnyType key or {@code NULL} if no type condition was found
     */
    public String hasAnyTypeCond() {
        String anyTypeName = null;

        if (type == null) {
            return anyTypeName;
        }

        switch (type) {
            case LEAF:
            case NOT_LEAF:
                if (anyTypeCond != null) {
                    anyTypeName = anyTypeCond.getAnyTypeKey();
                }
                break;

            case AND:
            case OR:
                if (leftSearchCond != null) {
                    anyTypeName = leftSearchCond.hasAnyTypeCond();
                }
                if (anyTypeName == null && rightSearchCond != null) {
                    anyTypeName = rightSearchCond.hasAnyTypeCond();
                }
                break;

            default:
        }

        return anyTypeName;
    }

    public AnyCond getAnyCond() {
        return anyCond;
    }

    public AttributeCond getAttributeCond() {
        return attributeCond;
    }

    public RelationshipCond getRelationshipCond() {
        return relationshipCond;
    }

    public RelationshipTypeCond getRelationshipTypeCond() {
        return relationshipTypeCond;
    }

    public MembershipCond getMembershipCond() {
        return membershipCond;
    }

    public RoleCond getRoleCond() {
        return roleCond;
    }

    public ResourceCond getResourceCond() {
        return resourceCond;
    }

    public AssignableCond getAssignableCond() {
        return assignableCond;
    }

    public MemberCond getMemberCond() {
        return memberCond;
    }

    public SearchCond getLeftSearchCond() {
        return leftSearchCond;
    }

    public SearchCond getRightSearchCond() {
        return rightSearchCond;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean isValid() {
        boolean isValid = false;

        if (type == null) {
            return isValid;
        }

        switch (type) {
            case LEAF:
            case NOT_LEAF:
                isValid = (anyTypeCond != null || anyCond != null || attributeCond != null
                        || relationshipCond != null || relationshipTypeCond != null || membershipCond != null
                        || roleCond != null || resourceCond != null || assignableCond != null || memberCond != null)
                        && (anyTypeCond == null || anyTypeCond.isValid())
                        && (anyCond == null || anyCond.isValid())
                        && (attributeCond == null || attributeCond.isValid())
                        && (membershipCond == null || membershipCond.isValid())
                        && (roleCond == null || roleCond.isValid())
                        && (resourceCond == null || resourceCond.isValid())
                        && (memberCond == null || memberCond.isValid());
                break;

            case AND:
            case OR:
                isValid = (leftSearchCond == null || rightSearchCond == null)
                        ? false
                        : leftSearchCond.isValid() && rightSearchCond.isValid();
                break;

            default:
        }

        return isValid;
    }
}
