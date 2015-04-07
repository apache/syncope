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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.AttrTemplate;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.Entitlement;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttr;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GDerSchema;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttr;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GVirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.validation.entity.GroupCheck;
import org.apache.syncope.core.persistence.jpa.entity.AbstractSubject;
import org.apache.syncope.core.persistence.jpa.entity.JPAAccountPolicy;
import org.apache.syncope.core.persistence.jpa.entity.JPAEntitlement;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPAPasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMDerAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMVirAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;

@Entity
@Table(name = JPAGroup.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "name", "parent_id" }))
@Cacheable
@GroupCheck
public class JPAGroup extends AbstractSubject<GPlainAttr, GDerAttr, GVirAttr> implements Group {

    private static final long serialVersionUID = -5281258853142421875L;

    public static final String TABLE = "SyncopeGroup";

    @Id
    private Long id;

    @NotNull
    private String name;

    @ManyToOne(optional = true)
    private JPAGroup parent;

    @ManyToOne(optional = true)
    private JPAUser userOwner;

    @ManyToOne(optional = true)
    private JPAGroup groupOwner;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "group_id"),
            inverseJoinColumns =
            @JoinColumn(name = "entitlement_name"))
    private Set<JPAEntitlement> entitlements;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGPlainAttrTemplate> rAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGDerAttrTemplate> rDerAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAGVirAttrTemplate> rVirAttrTemplates;

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

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritOwner;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritTemplates;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritPlainAttrs;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritDerAttrs;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritVirAttrs;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritPasswordPolicy;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritAccountPolicy;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private JPAPasswordPolicy passwordPolicy;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private JPAAccountPolicy accountPolicy;

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

    public JPAGroup() {
        super();

        entitlements = new HashSet<>();

        rAttrTemplates = new ArrayList<>();
        rDerAttrTemplates = new ArrayList<>();
        rVirAttrTemplates = new ArrayList<>();
        mAttrTemplates = new ArrayList<>();
        mDerAttrTemplates = new ArrayList<>();
        mVirAttrTemplates = new ArrayList<>();

        plainAttrs = new ArrayList<>();
        derAttrs = new ArrayList<>();
        virAttrs = new ArrayList<>();

        inheritOwner = getBooleanAsInteger(false);
        inheritTemplates = getBooleanAsInteger(false);
        inheritPlainAttrs = getBooleanAsInteger(false);
        inheritDerAttrs = getBooleanAsInteger(false);
        inheritVirAttrs = getBooleanAsInteger(false);
        inheritPasswordPolicy = getBooleanAsInteger(false);
        inheritAccountPolicy = getBooleanAsInteger(false);

        resources = new HashSet<>();
    }

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    protected Set<? extends ExternalResource> internalGetResources() {
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
    public Group getParent() {
        return parent;
    }

    @Override
    public void setParent(final Group parent) {
        checkType(parent, JPAGroup.class);
        this.parent = (JPAGroup) parent;
    }

    @Override
    public boolean isInheritOwner() {
        return isBooleanAsInteger(inheritOwner);
    }

    @Override
    public void setInheritOwner(final boolean inheritOwner) {
        this.inheritOwner = getBooleanAsInteger(inheritOwner);
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
    public boolean addEntitlement(final Entitlement entitlement) {
        checkType(entitlement, JPAEntitlement.class);
        return entitlements.add((JPAEntitlement) entitlement);
    }

    @Override
    public boolean removeEntitlement(final Entitlement entitlement) {
        checkType(entitlement, JPAEntitlement.class);
        return entitlements.remove((JPAEntitlement) entitlement);
    }

    @Override
    public Set<? extends Entitlement> getEntitlements() {
        return entitlements;
    }

    @Override
    public boolean isInheritTemplates() {
        return isBooleanAsInteger(inheritTemplates);
    }

    @Override
    public void setInheritTemplates(final boolean inheritAttrTemplates) {
        this.inheritTemplates = getBooleanAsInteger(inheritAttrTemplates);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AttrTemplate<K>, K extends Schema> List<T> getAttrTemplates(final Class<T> reference) {
        List<T> result = new ArrayList<>();

        if (GPlainAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) rAttrTemplates;
        } else if (GDerAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) rDerAttrTemplates;
        } else if (GVirAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) rVirAttrTemplates;
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

        return CollectionUtils.find(findInheritedTemplates(reference), new Predicate<T>() {

            @Override
            public boolean evaluate(final T template) {
                return schemaName.equals(template.getSchema().getKey());
            }
        });
    }

    @Override
    public <T extends AttrTemplate<K>, K extends Schema> List<K> getAttrTemplateSchemas(final Class<T> reference) {
        return CollectionUtils.collect(findInheritedTemplates(reference), new Transformer<T, K>() {

            @Override
            public K transform(final T input) {
                return input.getSchema();
            }
        }, new ArrayList<K>());
    }

    @Override
    public <T extends AttrTemplate<K>, K extends Schema> List<T> findInheritedTemplates(final Class<T> reference) {
        final List<T> result = new ArrayList<>(getAttrTemplates(reference));

        if (isInheritTemplates() && getParent() != null) {
            result.addAll(getParent().findInheritedTemplates(reference));
        }

        return result;
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
    public boolean isInheritPlainAttrs() {
        return isBooleanAsInteger(inheritPlainAttrs);
    }

    @Override
    public void setInheritPlainAttrs(final boolean inheritPlainAttrs) {
        this.inheritPlainAttrs = getBooleanAsInteger(inheritPlainAttrs);
    }

    /**
     * Get all inherited attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    @Override
    public List<? extends GPlainAttr> findLastInheritedAncestorPlainAttrs() {
        if (!isInheritPlainAttrs()) {
            return plainAttrs;
        }

        final Map<JPAGPlainSchema, GPlainAttr> result = new HashMap<>();
        if (isInheritPlainAttrs() && getParent() != null) {
            final Map<PlainSchema, GPlainAttr> attrMap = getPlainAttrMap();

            // Add inherit attributes
            for (GPlainAttr attr : getParent().findLastInheritedAncestorPlainAttrs()) {
                if (attrMap.containsKey(attr.getSchema())) {
                    result.remove((JPAGPlainSchema) attr.getSchema());
                }
                result.put((JPAGPlainSchema) attr.getSchema(), attr);
            }
        }
        return new ArrayList<>(result.values());
    }

    @Override
    public boolean isInheritDerAttrs() {
        return isBooleanAsInteger(inheritDerAttrs);
    }

    @Override
    public void setInheritDerAttrs(final boolean inheritDerAttrs) {
        this.inheritDerAttrs = getBooleanAsInteger(inheritDerAttrs);

    }

    /**
     * Get all inherited derived attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    @Override
    public List<? extends GDerAttr> findLastInheritedAncestorDerAttrs() {
        if (!isInheritDerAttrs()) {
            return derAttrs;
        }

        final Map<GDerSchema, GDerAttr> result = new HashMap<>();
        if (isInheritDerAttrs() && getParent() != null) {
            Map<DerSchema, GDerAttr> derAttrMap = getDerAttrMap();

            // Add inherit derived attributes
            for (GDerAttr attr : getParent().findLastInheritedAncestorDerAttrs()) {
                if (derAttrMap.containsKey(attr.getSchema())) {
                    result.remove(attr.getSchema());
                }
                result.put(attr.getSchema(), attr);
            }
        }
        return new ArrayList<>(result.values());
    }

    @Override
    public boolean isInheritVirAttrs() {
        return isBooleanAsInteger(inheritVirAttrs);
    }

    @Override
    public void setInheritVirAttrs(final boolean inheritVirAttrs) {
        this.inheritVirAttrs = getBooleanAsInteger(inheritVirAttrs);

    }

    /**
     * Get all inherited virtual attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    @Override
    public List<? extends GVirAttr> findLastInheritedAncestorVirAttrs() {
        if (!isInheritVirAttrs()) {
            return virAttrs;
        }

        final Map<GVirSchema, GVirAttr> result = new HashMap<>();
        if (isInheritVirAttrs() && getParent() != null) {
            Map<VirSchema, GVirAttr> virAttrMap = getVirAttrMap();

            // Add inherit virtual attributes
            for (GVirAttr attr : getParent().findLastInheritedAncestorVirAttrs()) {
                if (virAttrMap.containsKey(attr.getSchema())) {
                    result.remove(attr.getSchema());
                }
                result.put(attr.getSchema(), attr);
            }
        }
        return new ArrayList<>(result.values());
    }

    /**
     * Get first valid password policy.
     *
     * @return parent password policy if isInheritPasswordPolicy is 'true' and parent is not null, local password policy
     * otherwise
     */
    @Override
    public PasswordPolicy getPasswordPolicy() {
        return isInheritPasswordPolicy() && getParent() != null
                ? getParent().getPasswordPolicy()
                : passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(final PasswordPolicy passwordPolicy) {
        checkType(passwordPolicy, JPAPasswordPolicy.class);
        this.passwordPolicy = (JPAPasswordPolicy) passwordPolicy;
    }

    @Override
    public boolean isInheritPasswordPolicy() {
        return isBooleanAsInteger(inheritPasswordPolicy);
    }

    @Override
    public void setInheritPasswordPolicy(final boolean inheritPasswordPolicy) {
        this.inheritPasswordPolicy = getBooleanAsInteger(inheritPasswordPolicy);
    }

    /**
     * Get first valid account policy.
     *
     * @return parent account policy if isInheritAccountPolicy is 'true' and parent is not null, local account policy
     * otherwise.
     */
    @Override
    public AccountPolicy getAccountPolicy() {
        return isInheritAccountPolicy() && getParent() != null
                ? getParent().getAccountPolicy()
                : accountPolicy;
    }

    @Override
    public void setAccountPolicy(final AccountPolicy accountPolicy) {
        checkType(accountPolicy, JPAAccountPolicy.class);
        this.accountPolicy = (JPAAccountPolicy) accountPolicy;
    }

    @Override
    public boolean isInheritAccountPolicy() {
        return isBooleanAsInteger(inheritAccountPolicy);
    }

    @Override
    public void setInheritAccountPolicy(boolean inheritAccountPolicy) {
        this.inheritAccountPolicy = getBooleanAsInteger(inheritAccountPolicy);
    }
}
