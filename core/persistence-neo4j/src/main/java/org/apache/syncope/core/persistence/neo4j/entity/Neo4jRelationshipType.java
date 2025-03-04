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
package org.apache.syncope.core.persistence.neo4j.entity;

import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.common.validation.RelationshipTypeCheck;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jRelationshipType.NODE)
@RelationshipTypeCheck
public class Neo4jRelationshipType extends AbstractProvidedKeyNode implements RelationshipType {

    private static final long serialVersionUID = -753673974614737065L;

    public static final String NODE = "RelationshipType";

    public static final String LEFT_END_ANYTYPE = "RELATIONSHIPTYPE_LEFT_END_ANYTYPE";

    public static final String RIGHT_END_ANYTYPE = "RELATIONSHIPTYPE_RIGHT_END_ANYTYPE";

    private String description;

    @NotNull
    @Relationship(type = LEFT_END_ANYTYPE, direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyType leftEndAnyType;

    @NotNull
    @Relationship(type = RIGHT_END_ANYTYPE, direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyType rightEndAnyType;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public AnyType getLeftEndAnyType() {
        return leftEndAnyType;
    }

    @Override
    public void setLeftEndAnyType(final AnyType anyType) {
        checkType(anyType, Neo4jAnyType.class);
        this.leftEndAnyType = (Neo4jAnyType) anyType;
    }

    @Override
    public AnyType getRightEndAnyType() {
        return rightEndAnyType;
    }

    @Override
    public void setRightEndAnyType(final AnyType anyType) {
        checkType(anyType, Neo4jAnyType.class);
        this.rightEndAnyType = (Neo4jAnyType) anyType;
    }
}
