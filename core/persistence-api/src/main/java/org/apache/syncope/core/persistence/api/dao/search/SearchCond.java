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

    private SearchCond leftNodeCond;

    private SearchCond rightNodeCond;

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

    public static SearchCond getNotLeafCond(final SearchCond nodeCond) {
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static SearchCond getAndCond(final SearchCond leftCond, final SearchCond rightCond) {
        SearchCond nodeCond = new SearchCond();

        nodeCond.type = Type.AND;
        nodeCond.leftNodeCond = leftCond;
        nodeCond.rightNodeCond = rightCond;

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
        nodeCond.leftNodeCond = leftCond;
        nodeCond.rightNodeCond = rightCond;

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

    public AnyCond getAnyCond() {
        return anyCond;
    }

    public void setAnyCond(final AnyCond anyCond) {
        this.anyCond = anyCond;
    }

    public AttributeCond getAttributeCond() {
        return attributeCond;
    }

    public void setAttributeCond(final AttributeCond attributeCond) {
        this.attributeCond = attributeCond;
    }

    public RelationshipCond getRelationshipCond() {
        return relationshipCond;
    }

    public void setRelationshipCond(final RelationshipCond relationshipCond) {
        this.relationshipCond = relationshipCond;
    }

    public RelationshipTypeCond getRelationshipTypeCond() {
        return relationshipTypeCond;
    }

    public void setRelationshipTypeCond(final RelationshipTypeCond relationshipTypeCond) {
        this.relationshipTypeCond = relationshipTypeCond;
    }

    public MembershipCond getMembershipCond() {
        return membershipCond;
    }

    public void setMembershipCond(final MembershipCond membershipCond) {
        this.membershipCond = membershipCond;
    }

    public RoleCond getRoleCond() {
        return roleCond;
    }

    public void setRoleCond(final RoleCond roleCond) {
        this.roleCond = roleCond;
    }

    public ResourceCond getResourceCond() {
        return resourceCond;
    }

    public void setResourceCond(final ResourceCond resourceCond) {
        this.resourceCond = resourceCond;
    }

    public AssignableCond getAssignableCond() {
        return assignableCond;
    }

    public void setAssignableCond(final AssignableCond assignableCond) {
        this.assignableCond = assignableCond;
    }

    public SearchCond getLeftNodeCond() {
        return leftNodeCond;
    }

    public void setLeftNodeCond(final SearchCond leftNodeCond) {
        this.leftNodeCond = leftNodeCond;
    }

    public SearchCond getRightNodeCond() {
        return rightNodeCond;
    }

    public void setRightNodeCond(final SearchCond rightNodeCond) {
        this.rightNodeCond = rightNodeCond;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

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
                if (leftNodeCond != null) {
                    anyTypeName = leftNodeCond.hasAnyTypeCond();
                }
                if (anyTypeName == null && rightNodeCond != null) {
                    anyTypeName = rightNodeCond.hasAnyTypeCond();
                }
                break;

            default:
        }

        return anyTypeName;
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
                        || roleCond != null || resourceCond != null || assignableCond != null)
                        && (anyTypeCond == null || anyTypeCond.isValid())
                        && (anyCond == null || anyCond.isValid())
                        && (attributeCond == null || attributeCond.isValid())
                        && (membershipCond == null || membershipCond.isValid())
                        && (roleCond == null || roleCond.isValid())
                        && (resourceCond == null || resourceCond.isValid());
                break;

            case AND:
            case OR:
                isValid = (leftNodeCond == null || rightNodeCond == null)
                        ? false
                        : leftNodeCond.isValid() && rightNodeCond.isValid();
                break;

            default:
        }

        return isValid;
    }
}
