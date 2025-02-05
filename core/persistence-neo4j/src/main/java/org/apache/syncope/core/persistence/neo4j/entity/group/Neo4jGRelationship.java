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
package org.apache.syncope.core.persistence.neo4j.entity.group;

import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.GRelationship;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.common.entity.AMembershipType;
import org.apache.syncope.core.persistence.common.entity.UMembershipType;
import org.apache.syncope.core.persistence.common.validation.RelationshipCheck;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGeneratedKeyNode;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRelationshipType;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jGRelationship.NODE)
@RelationshipCheck
public class Neo4jGRelationship extends AbstractGeneratedKeyNode implements GRelationship {

    private static final long serialVersionUID = 2778494939240083204L;

    public static final String NODE = "GRelationship";

    public static final String SOURCE_REL = "GRELATIONSHIP_SOURCE";

    public static final String DEST_REL = "GRELATIONSHIP_DEST";

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jRelationshipType type;

    @Relationship(type = SOURCE_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jGroup leftEnd;

    @Relationship(type = DEST_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyObject rightEnd;

    @Override
    public RelationshipType getType() {
        return type;
    }

    @Override
    public void setType(final RelationshipType type) {
        if (UMembershipType.KEY.equalsIgnoreCase(type.getKey())
                || AMembershipType.KEY.equalsIgnoreCase(type.getKey())) {

            throw new IllegalArgumentException("This is not a membership");
        }
        checkType(type, Neo4jRelationshipType.class);
        this.type = (Neo4jRelationshipType) type;
    }

    @Override
    public Neo4jGroup getLeftEnd() {
        return leftEnd;
    }

    @Override
    public void setLeftEnd(final Group leftEnd) {
        checkType(leftEnd, Neo4jGroup.class);
        this.leftEnd = (Neo4jGroup) leftEnd;
    }

    @Override
    public AnyObject getRightEnd() {
        return rightEnd;
    }

    @Override
    public void setRightEnd(final AnyObject rightEnd) {
        checkType(rightEnd, Neo4jAnyObject.class);
        this.rightEnd = (Neo4jAnyObject) rightEnd;
    }
}
