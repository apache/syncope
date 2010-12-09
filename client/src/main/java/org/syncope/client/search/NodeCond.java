/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.client.search;

import org.syncope.client.AbstractBaseBean;

public class NodeCond extends AbstractBaseBean {

    public enum Type {

        LEAF, NOT_LEAF, AND, OR

    }
    private Type type;

    private AttributeCond attributeCond;

    private MembershipCond membershipCond;

    private NodeCond leftNodeCond;

    private NodeCond rightNodeCond;

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

    public static NodeCond getAndCond(final NodeCond leftCond,
            final NodeCond rightCond) {

        NodeCond nodeCond = new NodeCond();

        nodeCond.type = Type.AND;
        nodeCond.leftNodeCond = leftCond;
        nodeCond.rightNodeCond = rightCond;

        return nodeCond;
    }

    public static NodeCond getOrCond(final NodeCond leftCond,
            final NodeCond rightCond) {

        NodeCond nodeCond = new NodeCond();

        nodeCond.type = Type.OR;
        nodeCond.leftNodeCond = leftCond;
        nodeCond.rightNodeCond = rightCond;

        return nodeCond;
    }

    public AttributeCond getAttributeCond() {
        return attributeCond;
    }

    public final void setAttributeCond(final AttributeCond attributeCond) {
        this.attributeCond = attributeCond;
    }

    public final MembershipCond getMembershipCond() {
        return membershipCond;
    }

    public final void setMembershipCond(final MembershipCond membershipCond) {
        this.membershipCond = membershipCond;
    }

    public final NodeCond getLeftNodeCond() {
        return leftNodeCond;
    }

    public final void setLeftNodeCond(final NodeCond leftNodeCond) {
        this.leftNodeCond = leftNodeCond;
    }

    public final NodeCond getRightNodeCond() {
        return rightNodeCond;
    }

    public final void setRightNodeCond(final NodeCond rightNodeCond) {
        this.rightNodeCond = rightNodeCond;
    }

    public final Type getType() {
        return type;
    }

    public final void setType(final Type type) {
        this.type = type;
    }

    public final boolean checkValidity() {
        if (type == null) {
            return false;
        }

        switch (type) {
            case LEAF:
            case NOT_LEAF:
                return (attributeCond != null && membershipCond == null
                        && attributeCond.checkValidity())
                        || (attributeCond == null && membershipCond != null
                        && membershipCond.checkValidity());
            case AND:
            case OR:
                return (leftNodeCond == null || rightNodeCond == null)
                        ? false
                        : leftNodeCond.checkValidity()
                        && rightNodeCond.checkValidity();
            default:
                return false;
        }
    }
}
