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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.core.persistence.api.entity.AttrTemplate;
import org.apache.syncope.core.persistence.api.entity.DynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttr;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttr;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.validation.entity.GroupCheck;
import org.apache.syncope.core.persistence.jpa.entity.AbstractSubject;
import org.apache.syncope.core.persistence.jpa.entity.JPADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMDerAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMVirAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;

@Entity
@Table(name = JPAGroup.TABLE)
@Cacheable
@GroupCheck
public class JPAGroup extends AbstractSubject<GPlainAttr, GDerAttr, GVirAttr> implements Group {

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
    private List<JPAGPlainAttrTemplate> gAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGDerAttrTemplate> gDerAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGVirAttrTemplate> gVirAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAMPlainAttrTemplate> mAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAMDerAttrTemplate> mDerAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAMVirAttrTemplate> mVirAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGPlainAttr> plainAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGDerAttr> derAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGVirAttr> virAttrs;

    /**
     * Provisioning external resources.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "group_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_name"))
    @Valid
    private Set<JPAExternalResource> resources;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "group")
    @Valid
    private JPADynGroupMembership dynMembership;

    public JPAGroup() {
        super();

        gAttrTemplates = new ArrayList<>();
        gDerAttrTemplates = new ArrayList<>();
        gVirAttrTemplates = new ArrayList<>();
        mAttrTemplates = new ArrayList<>();
        mDerAttrTemplates = new ArrayList<>();
        mVirAttrTemplates = new ArrayList<>();

        plainAttrs = new ArrayList<>();
        derAttrs = new ArrayList<>();
        virAttrs = new ArrayList<>();

        resources = new HashSet<>();
    }

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    protected Set<JPAExternalResource> internalGetResources() {
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
    @SuppressWarnings("unchecked")
    public <T extends AttrTemplate<K>, K extends Schema> List<T> getAttrTemplates(final Class<T> reference) {
        List<T> result = new ArrayList<>();

        if (GPlainAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) gAttrTemplates;
        } else if (GDerAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) gDerAttrTemplates;
        } else if (GVirAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) gVirAttrTemplates;
        } else if (MPlainAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) mAttrTemplates;
        } else if (MDerAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) mDerAttrTemplates;
        } else if (MVirAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) mVirAttrTemplates;
        }

        return result;
    }

    @Override
    public <T extends AttrTemplate<K>, K extends Schema> T getAttrTemplate(
            final Class<T> reference, final String schemaName) {

        return CollectionUtils.find(getAttrTemplates(reference), new Predicate<T>() {

            @Override
            public boolean evaluate(final T template) {
                return schemaName.equals(template.getSchema().getKey());
            }
        });
    }

    @Override
    public <T extends AttrTemplate<K>, K extends Schema> List<K> getAttrTemplateSchemas(final Class<T> reference) {
        return CollectionUtils.collect(getAttrTemplates(reference), new Transformer<T, K>() {

            @Override
            public K transform(final T input) {
                return input.getSchema();
            }
        }, new ArrayList<K>());
    }

    @Override
    public boolean addPlainAttr(final GPlainAttr attr) {
        checkType(attr, JPAGPlainAttr.class);
        return plainAttrs.add((JPAGPlainAttr) attr);
    }

    @Override
    public boolean removePlainAttr(final GPlainAttr attr) {
        checkType(attr, JPAGPlainAttr.class);
        return plainAttrs.remove((JPAGPlainAttr) attr);
    }

    @Override
    public List<? extends GPlainAttr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    public boolean addDerAttr(final GDerAttr attr) {
        checkType(attr, JPAGDerAttr.class);
        return derAttrs.add((JPAGDerAttr) attr);
    }

    @Override
    public boolean removeDerAttr(final GDerAttr attr) {
        checkType(attr, JPAGDerAttr.class);
        return derAttrs.remove((JPAGDerAttr) attr);
    }

    @Override
    public List<? extends GDerAttr> getDerAttrs() {
        return derAttrs;
    }

    @Override
    public boolean addVirAttr(final GVirAttr attr) {
        checkType(attr, JPAGVirAttr.class);
        return virAttrs.add((JPAGVirAttr) attr);
    }

    @Override
    public boolean removeVirAttr(final GVirAttr attr) {
        checkType(attr, JPAGVirAttr.class);
        return virAttrs.remove((JPAGVirAttr) attr);
    }

    @Override
    public List<? extends GVirAttr> getVirAttrs() {
        return virAttrs;
    }

    @Override
    public DynGroupMembership getDynMembership() {
        return dynMembership;
    }

    @Override
    public void setDynMembership(final DynGroupMembership dynMembership) {
        checkType(dynMembership, JPADynGroupMembership.class);
        this.dynMembership = (JPADynGroupMembership) dynMembership;
    }

}
