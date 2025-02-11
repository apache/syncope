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
package org.apache.syncope.core.persistence.jpa.entity.anyobject;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.common.validation.AnyObjectCheck;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGroupableRelatable;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;

@Entity
@Table(name = JPAAnyObject.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "name", "type_id" }))
@EntityListeners({ JSONAnyObjectListener.class })
@Cacheable
@AnyObjectCheck
public class JPAAnyObject
        extends AbstractGroupableRelatable<AnyObject, AMembership, AnyObject, ARelationship>
        implements AnyObject {

    private static final long serialVersionUID = 9063766472970643492L;

    public static final String TABLE = "AnyObject";

    @Size(min = 1)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPAAnyType type;

    private String plainAttrs;

    @Transient
    private final List<PlainAttr> plainAttrsList = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "anyObject_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "anyObject_id", "resource_id" }))
    private List<JPAExternalResource> resources = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(joinColumns =
            @JoinColumn(name = "anyObject_id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "anyObject_id", "anyTypeClass_id" }))
    private List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "leftEnd")
    @Valid
    private List<JPAARelationship> relationships = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "leftEnd")
    @Valid
    private List<JPAAMembership> memberships = new ArrayList<>();

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
        checkType(type, JPAAnyType.class);
        this.type = (JPAAnyType) type;
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
        return plainAttrsList.removeIf(a -> a.getSchema().equals(attr.getSchema())
                && Objects.equals(a.getMembership(), attr.getMembership()));
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
    public boolean add(final ARelationship relationship) {
        checkType(relationship, JPAARelationship.class);
        return this.relationships.add((JPAARelationship) relationship);
    }

    @Override
    public List<? extends ARelationship> getRelationships() {
        return relationships;
    }

    @Override
    public boolean add(final AMembership membership) {
        checkType(membership, JPAAMembership.class);
        return this.memberships.add((JPAAMembership) membership);
    }

    @Override
    public boolean remove(final AMembership membership) {
        checkType(membership, JPAAMembership.class);
        plainAttrsList.removeIf(attr -> Objects.equals(attr.getMembership(), membership.getKey()));
        return this.memberships.remove((JPAAMembership) membership);
    }

    @Override
    public List<? extends AMembership> getMemberships() {
        return memberships;
    }
}
