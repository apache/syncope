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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.GRelationship;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.common.validation.AttributableCheck;
import org.apache.syncope.core.persistence.common.validation.GroupCheck;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractRelatable;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jADynGroupMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUDynGroupMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jGroup.NODE)
@GroupCheck
@AttributableCheck
public class Neo4jGroup
        extends AbstractRelatable<Group, AnyObject, GRelationship>
        implements Group {

    private static final long serialVersionUID = -5281258853142421875L;

    public static final String NODE = "SyncopeGroup";

    public static final String USER_OWNER_REL = "USER_OWNER";

    public static final String GROUP_OWNER_REL = "GROUP_OWNER";

    public static final String GROUP_RESOURCE_REL = "GROUP_RESOURCE";

    public static final String GROUP_AUX_CLASSES_REL = "GROUP_AUX_CLASSES";

    public static final String GROUP_TYPE_EXTENSION_REL = "GROUP_TYPE_EXTENSION";

    @NotNull
    private String name;

    @CompositeProperty(converterRef = "plainAttrsConverter")
    protected Map<String, PlainAttr> plainAttrs = new HashMap<>();

    @Relationship(type = USER_OWNER_REL, direction = Relationship.Direction.OUTGOING)
    protected Neo4jUser userOwner;

    @Relationship(type = GROUP_OWNER_REL, direction = Relationship.Direction.OUTGOING)
    protected Neo4jGroup groupOwner;

    /**
     * Provisioning external resources.
     */
    @Relationship(type = GROUP_RESOURCE_REL, direction = Relationship.Direction.OUTGOING)
    protected List<Neo4jExternalResource> resources = new ArrayList<>();

    @Relationship(type = GROUP_AUX_CLASSES_REL, direction = Relationship.Direction.OUTGOING)
    protected List<Neo4jAnyTypeClass> auxClasses = new ArrayList<>();

    @Relationship(direction = Relationship.Direction.INCOMING)
    private Neo4jUDynGroupMembership uDynMembership;

    @Relationship(direction = Relationship.Direction.INCOMING)
    private List<Neo4jADynGroupMembership> aDynMemberships = new ArrayList<>();

    @Relationship(type = GROUP_TYPE_EXTENSION_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jTypeExtension> typeExtensions = new ArrayList<>();

    @Relationship(type = Neo4jGRelationship.SOURCE_REL, direction = Relationship.Direction.INCOMING)
    protected List<Neo4jGRelationship> relationships = new ArrayList<>();

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
        return ApplicationContextProvider.getBeanFactory().getBean(AnyTypeDAO.class).getGroup();
    }

    @Override
    public void setType(final AnyType type) {
        // nothing to do
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
    public User getUserOwner() {
        return userOwner;
    }

    @Override
    public void setUserOwner(final User userOwner) {
        checkType(userOwner, Neo4jUser.class);
        this.userOwner = (Neo4jUser) userOwner;
    }

    @Override
    public Group getGroupOwner() {
        return groupOwner;
    }

    @Override
    public void setGroupOwner(final Group group) {
        checkType(group, Neo4jGroup.class);
        this.groupOwner = (Neo4jGroup) group;
    }

    @Override
    public boolean add(final PlainAttr attr) {
        return plainAttrs.put(attr.getSchema(), attr) != null;
    }

    @Override
    public boolean remove(final PlainAttr attr) {
        return plainAttrs.put(attr.getSchema(), null) != null;
    }

    @Override
    public List<PlainAttr> getPlainAttrs() {
        return plainAttrs.entrySet().stream().
                filter(e -> e.getValue() != null).
                sorted(Comparator.comparing(Map.Entry::getKey)).
                map(Map.Entry::getValue).toList();
    }

    @Override
    public UDynGroupMembership getUDynMembership() {
        return uDynMembership;
    }

    @Override
    public void setUDynMembership(final UDynGroupMembership uDynMembership) {
        checkType(uDynMembership, Neo4jUDynGroupMembership.class);
        this.uDynMembership = (Neo4jUDynGroupMembership) uDynMembership;
    }

    @Override
    public boolean add(final AnyTypeClass auxClass) {
        checkType(auxClass, Neo4jAnyTypeClass.class);
        return auxClasses.contains((Neo4jAnyTypeClass) auxClass) || this.auxClasses.add((Neo4jAnyTypeClass) auxClass);
    }

    @Override
    public List<? extends AnyTypeClass> getAuxClasses() {
        return auxClasses;
    }

    @Override
    public boolean add(final ADynGroupMembership dynGroupMembership) {
        checkType(dynGroupMembership, Neo4jADynGroupMembership.class);
        return this.aDynMemberships.add((Neo4jADynGroupMembership) dynGroupMembership);
    }

    @Override
    public Optional<? extends ADynGroupMembership> getADynMembership(final AnyType anyType) {
        return aDynMemberships.stream().
                filter(dynGroupMembership -> anyType != null && anyType.equals(dynGroupMembership.getAnyType())).
                findFirst();
    }

    @Override
    public List<? extends ADynGroupMembership> getADynMemberships() {
        return aDynMemberships;
    }

    @Override
    public boolean add(final TypeExtension typeExtension) {
        checkType(typeExtension, Neo4jTypeExtension.class);
        return this.typeExtensions.add((Neo4jTypeExtension) typeExtension);
    }

    @Override
    public Optional<? extends TypeExtension> getTypeExtension(final AnyType anyType) {
        return typeExtensions.stream().
                filter(typeExtension -> typeExtension.getAnyType().equals(anyType)).
                findFirst();
    }

    @Override
    public List<? extends TypeExtension> getTypeExtensions() {
        return typeExtensions;
    }

    @Override
    public boolean add(final GRelationship relationship) {
        checkType(relationship, Neo4jGRelationship.class);
        return this.relationships.add((Neo4jGRelationship) relationship);
    }

    @Override
    public List<? extends GRelationship> getRelationships() {
        return relationships;
    }
}
