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

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGeneratedKeyNode;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jTypeExtension.NODE)
public class Neo4jTypeExtension extends AbstractGeneratedKeyNode implements TypeExtension {

    private static final long serialVersionUID = -8367626793791263551L;

    public static final String NODE = "TypeExtension";

    @Relationship(type = Neo4jGroup.GROUP_TYPE_EXTENSION_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jGroup group;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyType anyType;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private List<Neo4jAnyTypeClass> auxClasses = new ArrayList<>();

    @Override
    public Group getGroup() {
        return group;
    }

    @Override
    public void setGroup(final Group group) {
        checkType(group, Neo4jGroup.class);
        this.group = (Neo4jGroup) group;
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

    @Override
    public boolean add(final AnyTypeClass auxClass) {
        checkType(auxClass, Neo4jAnyTypeClass.class);
        return auxClasses.contains((Neo4jAnyTypeClass) auxClass) || auxClasses.add((Neo4jAnyTypeClass) auxClass);
    }

    @Override
    public List<? extends AnyTypeClass> getAuxClasses() {
        return auxClasses;
    }
}
