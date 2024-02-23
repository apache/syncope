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
package org.apache.syncope.core.persistence.neo4j.entity.anyobject;

import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractDynMembership;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jADynGroupMembership.NODE)
public class Neo4jADynGroupMembership extends AbstractDynMembership<AnyObject> implements ADynGroupMembership {

    private static final long serialVersionUID = -7336814163949640354L;

    public static final String NODE = "ADynGroupMembership";

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jGroup group;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyType anyType;

    @Override
    public Group getGroup() {
        return group;
    }

    @Override
    public void setGroup(final Group role) {
        checkType(role, Neo4jGroup.class);
        this.group = (Neo4jGroup) role;
    }

    @Override
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, Neo4jAnyType.class);
        this.anyType = (Neo4jAnyType) anyType;
    }
}
