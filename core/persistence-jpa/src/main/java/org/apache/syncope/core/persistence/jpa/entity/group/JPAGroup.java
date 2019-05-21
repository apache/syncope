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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractAny;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.validation.entity.GroupCheck;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;

@Entity
@Table(name = JPAGroup.TABLE)
@Cacheable
@GroupCheck
public class JPAGroup extends AbstractAny<GPlainAttr> implements Group {

    private static final long serialVersionUID = -5281258853142421875L;

    public static final String TABLE = "SyncopeGroup";

    @Column(unique = true)
    @NotNull
    private String name;

    protected User userOwner;

    protected Group groupOwner;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")
    @Valid
    private List<JPAGPlainAttr> plainAttrs = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(joinColumns =
            @JoinColumn(name = "group_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = { "group_id", "resource_id" }))
    private List<JPAExternalResource> resources = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(joinColumns =
            @JoinColumn(name = "group_id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = { "group_id", "anyTypeClass_id" }))
    private List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "group")
    @Valid
    private JPAUDynGroupMembership uDynMembership;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "group")
    private List<JPAADynGroupMembership> aDynMemberships = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "group")
    private List<JPATypeExtension> typeExtensions = new ArrayList<>();

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
        return ApplicationContextProvider.getBeanFactory().getBean(AnyTypeDAO.class).findGroup();
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
    public boolean add(final GPlainAttr attr) {
        checkType(attr, JPAGPlainAttr.class);
        return plainAttrs.add((JPAGPlainAttr) attr);
    }

    @Override
    public boolean remove(final GPlainAttr attr) {
        checkType(attr, JPAGPlainAttr.class);
        return getPlainAttrs().remove((JPAGPlainAttr) attr);
    }

    @Override
    public Optional<? extends GPlainAttr> getPlainAttr(final String plainSchema) {
        return getPlainAttrs().stream().filter(plainAttr
                -> plainAttr != null && plainAttr.getSchema() != null
                && plainSchema.equals(plainAttr.getSchema().getKey())).findFirst();
    }

    @Override
    public List<? extends GPlainAttr> getPlainAttrs() {
        return plainAttrs;
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
}
