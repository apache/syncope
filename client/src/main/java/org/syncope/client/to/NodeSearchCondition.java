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
package org.syncope.client.to;

public class NodeSearchCondition extends AbstractBaseTO {

    public enum Type {

        LEAF, AND, OR, NOT
    }
    private Type type;
    private LeafSearchCondition leafSearchCondition;
    private NodeSearchCondition leftNodeSearchCondition;
    private NodeSearchCondition rightNodeSearchCondition;

    public static NodeSearchCondition getLeafCondition(
            LeafSearchCondition leafSearchCondition) {

        NodeSearchCondition nodeSearchCondition =
                new NodeSearchCondition();

        nodeSearchCondition.type = Type.LEAF;
        nodeSearchCondition.leafSearchCondition = leafSearchCondition;

        return nodeSearchCondition;
    }

    public static NodeSearchCondition getAndSearchCondition(
            NodeSearchCondition leftNodeSearchCondition,
            NodeSearchCondition rightNodeSearchCondition) {

        NodeSearchCondition nodeSearchCondition =
                new NodeSearchCondition();

        nodeSearchCondition.type = Type.AND;
        nodeSearchCondition.leftNodeSearchCondition = leftNodeSearchCondition;
        nodeSearchCondition.rightNodeSearchCondition = rightNodeSearchCondition;

        return nodeSearchCondition;
    }

    public static NodeSearchCondition getOrSearchCondition(
            NodeSearchCondition leftNodeSearchCondition,
            NodeSearchCondition rightNodeSearchCondition) {

        NodeSearchCondition nodeSearchCondition =
                new NodeSearchCondition();

        nodeSearchCondition.type = Type.OR;
        nodeSearchCondition.leftNodeSearchCondition = leftNodeSearchCondition;
        nodeSearchCondition.rightNodeSearchCondition = rightNodeSearchCondition;

        return nodeSearchCondition;
    }

    public static NodeSearchCondition getNotSearchCondition(
            NodeSearchCondition leftNodeSearchCondition) {

        NodeSearchCondition nodeSearchCondition =
                new NodeSearchCondition();

        nodeSearchCondition.type = Type.NOT;
        nodeSearchCondition.leftNodeSearchCondition = leftNodeSearchCondition;

        return nodeSearchCondition;
    }

    public void setLeafSearchCondition(LeafSearchCondition leafSearchCondition) {
        this.leafSearchCondition = leafSearchCondition;
    }

    public LeafSearchCondition getLeafSearchCondition() {
        return leafSearchCondition;
    }

    public void setLeftNodeSearchCondition(NodeSearchCondition leftNodeSearchCondition) {
        this.leftNodeSearchCondition = leftNodeSearchCondition;
    }

    public NodeSearchCondition getLeftNodeSearchCondition() {
        return leftNodeSearchCondition;
    }

    public void setRightNodeSearchCondition(NodeSearchCondition rightNodeSearchCondition) {
        this.rightNodeSearchCondition = rightNodeSearchCondition;
    }

    public NodeSearchCondition getRightNodeSearchCondition() {
        return rightNodeSearchCondition;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public boolean checkValidity() {
        if (type == null) {
            return false;
        }

        switch (type) {
            case LEAF:
                return leafSearchCondition.checkValidity();
            case AND:
            case OR:
                return (leftNodeSearchCondition == null
                        || rightNodeSearchCondition == null)
                        ? false
                        : leftNodeSearchCondition.checkValidity()
                        && rightNodeSearchCondition.checkValidity();
            case NOT:
                return leftNodeSearchCondition == null ? false
                        : leftNodeSearchCondition.checkValidity();
        }

        return false;
    }
}
