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

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.common.validation.AnyObjectCheck;
import org.apache.syncope.core.persistence.common.validation.AttributableCheck;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGroupableRelatable;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jAnyObject.NODE)
@AnyObjectCheck
@AttributableCheck
public class Neo4jAnyObject
        extends AbstractGroupableRelatable<AnyObject, AMembership, AnyObject, ARelationship>
        implements AnyObject {

    private static final long serialVersionUID = -3905046855521446823L;

    public static final String NODE = "AnyObject";

    public static final String ANY_OBJECT_GROUP_MEMBERSHIP_REL = "ANY_OBJECT_GROUP_MEMBERSHIP";

    public static final String ANY_OBJECT_RESOURCE_REL = "ANY_OBJECT_RESOURCE";

    public static final String ANY_OBJECT_AUX_CLASSES_REL = "ANY_OBJECT_AUX_CLASSES";

    @CompositeProperty(converterRef = "plainAttrsConverter")
    protected Map<String, PlainAttr> plainAttrs = new HashMap<>();

    @NotNull(message = "Blank name")
    protected String name;

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    protected Neo4jAnyType type;

    /**
     * Provisioning external resources.
     */
    @Relationship(type = ANY_OBJECT_RESOURCE_REL, direction = Relationship.Direction.OUTGOING)
    protected List<Neo4jExternalResource> resources = new ArrayList<>();

    @Relationship(type = ANY_OBJECT_AUX_CLASSES_REL, direction = Relationship.Direction.OUTGOING)
    protected List<Neo4jAnyTypeClass> auxClasses = new ArrayList<>();

    @Relationship(type = Neo4jARelationship.SOURCE_REL, direction = Relationship.Direction.INCOMING)
    protected List<Neo4jARelationship> relationships = new ArrayList<>();

    @Relationship(type = ANY_OBJECT_GROUP_MEMBERSHIP_REL, direction = Relationship.Direction.INCOMING)
    protected List<Neo4jAMembership> memberships = new ArrayList<>();

    @Override
    protected Map<String, PlainAttr> plainAttrs() {
        return plainAttrs;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public AnyType getType() {
        return type;
    }

    @Override
    public void setType(final AnyType type) {
        checkType(type, Neo4jAnyType.class);
        this.type = (Neo4jAnyType) type;
    }

    @Override
    public boolean add(final ExternalResource resource) {
        checkType(resource, Neo4jExternalResource.class);
        return resources.contains((Neo4jExternalResource) resource) || resources.add((Neo4jExternalResource) resource);
    }

    @Override
    public List<? extends ExternalResource> getResources() {
        return resources;
    }

    @Override
    public boolean add(final PlainAttr attr) {
        if (attr.getMembership() == null) {
            return plainAttrs.put(attr.getSchema(), attr) != null;
        }

        return memberships().stream().
                filter(membership -> membership.getKey().equals(attr.getMembership())).findFirst().
                map(membership -> membership.add(attr)).
                orElse(false);
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

    @Override
    public boolean add(final ARelationship relationship) {
        checkType(relationship, Neo4jARelationship.class);
        return this.relationships.add((Neo4jARelationship) relationship);
    }

    @Override
    public List<? extends ARelationship> getRelationships() {
        return relationships;
    }

    @Override
    public boolean add(final AMembership membership) {
        checkType(membership, Neo4jAMembership.class);
        return this.memberships.add((Neo4jAMembership) membership);
    }

    @Override
    public boolean remove(final AMembership membership) {
        checkType(membership, Neo4jAMembership.class);
        return this.memberships.remove((Neo4jAMembership) membership);
    }

    @Override
    protected List<Neo4jAMembership> memberships() {
        return memberships;
    }
}
