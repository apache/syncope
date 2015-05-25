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
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
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
public class JPAGroup extends AbstractAny<GPlainAttr, GDerAttr, GVirAttr> implements Group {

    private static final long serialVersionUID = -5281258853142421875L;

    public static final String TABLE = "SyncopeGroup";

    @Id
    private Long id;

    @Column(unique = true)
    @NotNull
    private String name;

    @ManyToOne
    private JPAUser userOwner;

    @ManyToOne
    private JPAGroup groupOwner;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGPlainAttr> plainAttrs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGDerAttr> derAttrs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGVirAttr> virAttrs = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "group_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_name"))
    @Valid
    private List<JPAExternalResource> resources = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "group_id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_name"))
    private List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "group")
    @Valid
    private JPAADynGroupMembership aDynMembership;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "group")
    @Valid
    private JPAUDynGroupMembership uDynMembership;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "group")
    private List<JPATypeExtension> typeExtensions = new ArrayList<>();

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public AnyType getType() {
        return ApplicationContextProvider.getApplicationContext().getBean(AnyTypeDAO.class).findGroup();
    }

    @Override
    public void setType(final AnyType type) {
        // nothing to do
    }

    @Override
    protected List<JPAExternalResource> internalGetResources() {
        return resources;
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
    public User getUserOwner() {
        return userOwner;
    }

    @Override
    public void setUserOwner(final User userOwner) {
        checkType(userOwner, JPAUser.class);
        this.userOwner = (JPAUser) userOwner;
    }

    @Override
    public JPAGroup getGroupOwner() {
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
        return plainAttrs.remove((JPAGPlainAttr) attr);
    }

    @Override
    public List<? extends GPlainAttr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    public boolean add(final GDerAttr attr) {
        checkType(attr, JPAGDerAttr.class);
        return derAttrs.add((JPAGDerAttr) attr);
    }

    @Override
    public boolean remove(final GDerAttr attr) {
        checkType(attr, JPAGDerAttr.class);
        return derAttrs.remove((JPAGDerAttr) attr);
    }

    @Override
    public List<? extends GDerAttr> getDerAttrs() {
        return derAttrs;
    }

    @Override
    public boolean add(final GVirAttr attr) {
        checkType(attr, JPAGVirAttr.class);
        return virAttrs.add((JPAGVirAttr) attr);
    }

    @Override
    public boolean remove(final GVirAttr attr) {
        checkType(attr, JPAGVirAttr.class);
        return virAttrs.remove((JPAGVirAttr) attr);
    }

    @Override
    public List<? extends GVirAttr> getVirAttrs() {
        return virAttrs;
    }

    @Override
    public ADynGroupMembership getADynMembership() {
        return aDynMembership;
    }

    @Override
    public void setADynMembership(final ADynGroupMembership aDynMembership) {
        checkType(aDynMembership, JPAADynGroupMembership.class);
        this.aDynMembership = (JPAADynGroupMembership) aDynMembership;
    }

    @Override
    public UDynGroupMembership getUDynMembership() {
        return uDynMembership;
    }

    @Override
    public void setUDynMembership(final UDynGroupMembership uDynMembership) {
        checkType(aDynMembership, JPAADynGroupMembership.class);
        this.uDynMembership = (JPAUDynGroupMembership) uDynMembership;
    }

    @Override
    public boolean add(final AnyTypeClass auxClass) {
        checkType(auxClass, JPAAnyTypeClass.class);
        return this.auxClasses.add((JPAAnyTypeClass) auxClass);
    }

    @Override
    public boolean remove(final AnyTypeClass auxClass) {
        checkType(auxClass, JPAAnyTypeClass.class);
        return this.auxClasses.remove((JPAAnyTypeClass) auxClass);
    }

    @Override
    public List<? extends AnyTypeClass> getAuxClasses() {
        return auxClasses;
    }

    @Override
    public boolean add(final TypeExtension typeExtension) {
        checkType(typeExtension, JPATypeExtension.class);
        return this.typeExtensions.add((JPATypeExtension) typeExtension);
    }

    @Override
    public boolean remove(final TypeExtension typeExtension) {
        checkType(typeExtension, JPATypeExtension.class);
        return this.typeExtensions.remove((JPATypeExtension) typeExtension);
    }

    @Override
    public TypeExtension getTypeExtension(final AnyType anyType) {
        return CollectionUtils.find(typeExtensions, new Predicate<TypeExtension>() {

            @Override
            public boolean evaluate(final TypeExtension typeExtension) {
                return typeExtension.getAnyType().equals(anyType);
            }
        });
    }

    @Override
    public List<? extends TypeExtension> getTypeExtensions() {
        return typeExtensions;
    }

}
