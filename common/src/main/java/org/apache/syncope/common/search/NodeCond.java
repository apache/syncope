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
package org.apache.syncope.common.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;

@XmlRootElement(name = "nodeCondition")
@XmlType
public class NodeCond extends AbstractBaseBean {

    private static final long serialVersionUID = 661560782247499526L;

    @XmlEnum
    @XmlType(name = "nodeConditionType")
    public enum Type {

        LEAF,
        NOT_LEAF,
        AND,
        OR

    }

    private Type type;

    private AttributableCond attributableCond;

    private AttributeCond attributeCond;

    private MembershipCond membershipCond;

    private ResourceCond resourceCond;

    private EntitlementCond entitlementCond;

    private NodeCond leftNodeCond;

    private NodeCond rightNodeCond;

    public static NodeCond getLeafCond(final AttributableCond syncopeUserCond) {
        NodeCond nodeCond = new NodeCond();

        nodeCond.type = Type.LEAF;
        nodeCond.attributableCond = syncopeUserCond;

        return nodeCond;
    }

    public static NodeCond getLeafCond(final AttributeCond attributeCond) {
        NodeCond nodeCond = new NodeCond();

        nodeCond.type = Type.LEAF;
        nodeCond.attributeCond = attributeCond;

        return nodeCond;
    }

    public static NodeCond getLeafCond(final MembershipCond membershipCond) {
        NodeCond nodeCond = new NodeCond();

        nodeCond.type = Type.LEAF;
        nodeCond.membershipCond = membershipCond;

        return nodeCond;
    }

    public static NodeCond getLeafCond(final ResourceCond resourceCond) {
        NodeCond nodeCond = new NodeCond();

        nodeCond.type = Type.LEAF;
        nodeCond.resourceCond = resourceCond;

        return nodeCond;
    }

    public static NodeCond getLeafCond(final EntitlementCond entitlementCond) {
        NodeCond nodeCond = new NodeCond();

        nodeCond.type = Type.LEAF;
        nodeCond.entitlementCond = entitlementCond;

        return nodeCond;
    }

    public static NodeCond getNotLeafCond(final AttributableCond syncopeUserCond) {
        NodeCond nodeCond = getLeafCond(syncopeUserCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static NodeCond getNotLeafCond(final AttributeCond attributeCond) {
        NodeCond nodeCond = getLeafCond(attributeCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static NodeCond getNotLeafCond(final MembershipCond membershipCond) {
        NodeCond nodeCond = getLeafCond(membershipCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static NodeCond getNotLeafCond(final ResourceCond resourceCond) {
        NodeCond nodeCond = getLeafCond(resourceCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static NodeCond getNotLeafCond(final EntitlementCond entitlementCond) {
        NodeCond nodeCond = getLeafCond(entitlementCond);
        nodeCond.type = Type.NOT_LEAF;
        return nodeCond;
    }

    public static NodeCond getAndCond(final NodeCond leftCond, final NodeCond rightCond) {
        NodeCond nodeCond = new NodeCond();

        nodeCond.type = Type.AND;
        nodeCond.leftNodeCond = leftCond;
        nodeCond.rightNodeCond = rightCond;

        return nodeCond;
    }

    public static NodeCond getOrCond(final NodeCond leftCond, final NodeCond rightCond) {
        NodeCond nodeCond = new NodeCond();

        nodeCond.type = Type.OR;
        nodeCond.leftNodeCond = leftCond;
        nodeCond.rightNodeCond = rightCond;

        return nodeCond;
    }

    public AttributableCond getAttributableCond() {
        return attributableCond;
    }

    public void setAttributableCond(final AttributableCond attributableCond) {
        this.attributableCond = attributableCond;
    }

    public AttributeCond getAttributeCond() {
        return attributeCond;
    }

    public void setAttributeCond(final AttributeCond attributeCond) {
        this.attributeCond = attributeCond;
    }

    public MembershipCond getMembershipCond() {
        return membershipCond;
    }

    public void setMembershipCond(final MembershipCond membershipCond) {
        this.membershipCond = membershipCond;
    }

    public ResourceCond getResourceCond() {
        return resourceCond;
    }

    public void setResourceCond(final ResourceCond resourceCond) {
        this.resourceCond = resourceCond;
    }

    public EntitlementCond getEntitlementCond() {
        return entitlementCond;
    }

    public void setEntitlementCond(final EntitlementCond entitlementCond) {
        this.entitlementCond = entitlementCond;
    }

    public NodeCond getLeftNodeCond() {
        return leftNodeCond;
    }

    public void setLeftNodeCond(final NodeCond leftNodeCond) {
        this.leftNodeCond = leftNodeCond;
    }

    public NodeCond getRightNodeCond() {
        return rightNodeCond;
    }

    public void setRightNodeCond(final NodeCond rightNodeCond) {
        this.rightNodeCond = rightNodeCond;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    @JsonIgnore
    public boolean isValid() {
        boolean isValid = false;

        if (type == null) {
            return isValid;
        }

        switch (type) {
            case LEAF:
            case NOT_LEAF:
                isValid = (attributableCond != null || attributeCond != null || membershipCond != null
                        || resourceCond != null || entitlementCond != null)
                        && (attributableCond == null || attributableCond.isValid())
                        && (attributeCond == null || attributeCond.isValid())
                        && (membershipCond == null || membershipCond.isValid())
                        && (resourceCond == null || resourceCond.isValid())
                        && (entitlementCond == null || entitlementCond.isValid());
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
