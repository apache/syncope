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
package org.apache.syncope.core.persistence.jpa.entity.group;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.syncope.core.persistence.common.validation.GroupCheck;
import org.apache.syncope.core.persistence.jpa.entity.AbstractRelatable;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;

@Entity
@Table(name = JPAGroup.TABLE)
@EntityListeners({ JSONGroupListener.class })
@Cacheable
@GroupCheck
public class JPAGroup
        extends AbstractRelatable<Group, AnyObject, GRelationship>
        implements Group {

    private static final long serialVersionUID = -5281258853142421875L;

    public static final String TABLE = "SyncopeGroup";

    @Column(unique = true)
    @NotNull
    private String name;

    @ManyToOne
    private JPAUser userOwner;

    @ManyToOne
    private JPAGroup groupOwner;

    private String plainAttrs;

    @Transient
    private final List<PlainAttr> plainAttrsList = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(joinColumns =
            @JoinColumn(name = "group_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "group_id", "resource_id" }))
    private List<JPAExternalResource> resources = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(joinColumns =
            @JoinColumn(name = "group_id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "group_id", "anyTypeClass_id" }))
    private List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "group")
    @Valid
    private JPAUDynGroupMembership uDynMembership;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "group")
    private List<JPAADynGroupMembership> aDynMemberships = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "group")
    private List<JPATypeExtension> typeExtensions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "leftEnd")
    @Valid
    private List<JPAGRelationship> relationships = new ArrayList<>();

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
        checkType(resource, JPAExternalResource.class);
        return resources.contains((JPAExternalResource) resource) || resources.add((JPAExternalResource) resource);
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
        checkType(userOwner, JPAUser.class);
        this.userOwner = (JPAUser) userOwner;
    }

    @Override
    public Group getGroupOwner() {
        return groupOwner;
    }

    @Override
    public void setGroupOwner(final Group group) {
        checkType(group, JPAGroup.class);
        this.groupOwner = (JPAGroup) group;
    }

    @Override
    public List<PlainAttr> getPlainAttrsList() {
        return plainAttrsList;
    }

    @Override
    public String getPlainAttrsJSON() {
        return plainAttrs;
    }

    @Override
    public void setPlainAttrsJSON(final String plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    @Override
    public boolean add(final PlainAttr attr) {
        return plainAttrsList.add(attr);
    }

    @Override
    public boolean remove(final PlainAttr attr) {
        return plainAttrsList.removeIf(a -> a.getSchema().equals(attr.getSchema()));
    }

    @Override
    public Optional<PlainAttr> getPlainAttr(final String plainSchema) {
        return plainAttrsList.stream().
                filter(attr -> plainSchema.equals(attr.getSchema())).
                findFirst();
    }

    @Override
    public List<PlainAttr> getPlainAttrs() {
        return plainAttrsList.stream().toList();
    }

    @Override
    public UDynGroupMembership getUDynMembership() {
        return uDynMembership;
    }

    @Override
    public void setUDynMembership(final UDynGroupMembership uDynMembership) {
        checkType(uDynMembership, JPAUDynGroupMembership.class);
        this.uDynMembership = (JPAUDynGroupMembership) uDynMembership;
    }

    @Override
    public boolean add(final AnyTypeClass auxClass) {
        checkType(auxClass, JPAAnyTypeClass.class);
        return auxClasses.contains((JPAAnyTypeClass) auxClass) || this.auxClasses.add((JPAAnyTypeClass) auxClass);
    }

    @Override
    public List<? extends AnyTypeClass> getAuxClasses() {
        return auxClasses;
    }

    @Override
    public boolean add(final ADynGroupMembership dynGroupMembership) {
        checkType(dynGroupMembership, JPAADynGroupMembership.class);
        return this.aDynMemberships.add((JPAADynGroupMembership) dynGroupMembership);
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
        checkType(typeExtension, JPATypeExtension.class);
        return this.typeExtensions.add((JPATypeExtension) typeExtension);
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
        checkType(relationship, JPAGRelationship.class);
        return this.relationships.add((JPAGRelationship) relationship);
    }

    @Override
    public List<? extends GRelationship> getRelationships() {
        return relationships;
    }
}
