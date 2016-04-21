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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.jpa.entity.AbstractAny;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;

@Entity
@Table(name = JPAAnyObject.TABLE)
@Cacheable
public class JPAAnyObject extends AbstractAny<APlainAttr> implements AnyObject {

    private static final long serialVersionUID = 9063766472970643492L;

    public static final String TABLE = "AnyObject";

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPAAnyType type;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAAPlainAttr> plainAttrs = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "anyObject_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_id"))
    private List<JPAExternalResource> resources = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "anyObject_id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_id"))
    private List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "leftEnd")
    @Valid
    private List<JPAARelationship> relationships = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "leftEnd")
    @Valid
    private List<JPAAMembership> memberships = new ArrayList<>();

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
    public boolean add(final APlainAttr attr) {
        checkType(attr, JPAAPlainAttr.class);
        return plainAttrs.add((JPAAPlainAttr) attr);
    }

    @Override
    public List<? extends APlainAttr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    protected List<JPAExternalResource> internalGetResources() {
        return resources;
    }

    @Override
    public boolean add(final AnyTypeClass auxClass) {
        checkType(auxClass, JPAAnyTypeClass.class);
        return this.auxClasses.add((JPAAnyTypeClass) auxClass);
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
    public ARelationship getRelationship(final RelationshipType relationshipType, final String anyObjectKey) {
        return IterableUtils.find(getRelationships(), new Predicate<ARelationship>() {

            @Override
            public boolean evaluate(final ARelationship relationship) {
                return anyObjectKey != null && anyObjectKey.equals(relationship.getRightEnd().getKey())
                        && ((relationshipType == null && relationship.getType() == null)
                        || (relationshipType != null && relationshipType.equals(relationship.getType())));
            }
        });
    }

    @Override
    public Collection<? extends ARelationship> getRelationships(final RelationshipType relationshipType) {
        return CollectionUtils.select(getRelationships(), new Predicate<ARelationship>() {

            @Override
            public boolean evaluate(final ARelationship relationship) {
                return relationshipType != null && relationshipType.equals(relationship.getType());
            }
        });
    }

    @Override
    public Collection<? extends ARelationship> getRelationships(final String anyObjectKey) {
        return CollectionUtils.select(getRelationships(), new Predicate<ARelationship>() {

            @Override
            public boolean evaluate(final ARelationship relationship) {
                return anyObjectKey != null && anyObjectKey.equals(relationship.getRightEnd().getKey());
            }
        });
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
    public AMembership getMembership(final String groupKey) {
        return IterableUtils.find(getMemberships(), new Predicate<AMembership>() {

            @Override
            public boolean evaluate(final AMembership membership) {
                return groupKey != null && groupKey.equals(membership.getRightEnd().getKey());
            }
        });
    }

    @Override
    public List<? extends AMembership> getMemberships() {
        return memberships;
    }
}
