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
package org.apache.syncope.core.persistence.beans.role;

import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrTemplate;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractNormalSchema;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirSchema;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.membership.MAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.validation.entity.SyncopeRoleCheck;

@Entity
@Table(uniqueConstraints =
        @UniqueConstraint(columnNames = { "name", "parent_id" }))
@Cacheable
@SyncopeRoleCheck
public class SyncopeRole extends AbstractAttributable {

    private static final long serialVersionUID = -5281258853142421875L;

    @Id
    private Long id;

    @NotNull
    private String name;

    @ManyToOne(optional = true)
    private SyncopeRole parent;

    @ManyToOne(optional = true)
    private SyncopeUser userOwner;

    @ManyToOne(optional = true)
    private SyncopeRole roleOwner;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "entitlement_name"))
    private Set<Entitlement> entitlements;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RAttrTemplate> rAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RDerAttrTemplate> rDerAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RVirAttrTemplate> rVirAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<MAttrTemplate> mAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<MDerAttrTemplate> mDerAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<MVirAttrTemplate> mVirAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RAttr> attrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RDerAttr> derAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RVirAttr> virAttrs;

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
    private Integer inheritAttrs;

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
    private PasswordPolicy passwordPolicy;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private AccountPolicy accountPolicy;

    /**
     * Provisioning external resources.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_name"))
    @Valid
    private Set<ExternalResource> resources;

    public SyncopeRole() {
        super();

        entitlements = new HashSet<Entitlement>();

        rAttrTemplates = new ArrayList<RAttrTemplate>();
        rDerAttrTemplates = new ArrayList<RDerAttrTemplate>();
        rVirAttrTemplates = new ArrayList<RVirAttrTemplate>();
        mAttrTemplates = new ArrayList<MAttrTemplate>();
        mDerAttrTemplates = new ArrayList<MDerAttrTemplate>();
        mVirAttrTemplates = new ArrayList<MVirAttrTemplate>();

        attrs = new ArrayList<RAttr>();
        derAttrs = new ArrayList<RDerAttr>();
        virAttrs = new ArrayList<RVirAttr>();

        inheritOwner = getBooleanAsInteger(false);
        inheritTemplates = getBooleanAsInteger(false);
        inheritAttrs = getBooleanAsInteger(false);
        inheritDerAttrs = getBooleanAsInteger(false);
        inheritVirAttrs = getBooleanAsInteger(false);
        inheritPasswordPolicy = getBooleanAsInteger(false);
        inheritAccountPolicy = getBooleanAsInteger(false);

        resources = new HashSet<ExternalResource>();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    protected Set<ExternalResource> internalGetResources() {
        return resources;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public SyncopeRole getParent() {
        return parent;
    }

    public void setParent(final SyncopeRole parent) {
        this.parent = parent;
    }

    public boolean isInheritOwner() {
        return isBooleanAsInteger(inheritOwner);
    }

    public void setInheritOwner(final boolean inheritOwner) {
        this.inheritOwner = getBooleanAsInteger(inheritOwner);
    }

    public SyncopeUser getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final SyncopeUser userOwner) {
        this.userOwner = userOwner;
    }

    public SyncopeRole getRoleOwner() {
        return roleOwner;
    }

    public void setRoleOwner(final SyncopeRole roleOwner) {
        this.roleOwner = roleOwner;
    }

    public boolean addEntitlement(final Entitlement entitlement) {
        return entitlements.add(entitlement);
    }

    public boolean removeEntitlement(final Entitlement entitlement) {
        return entitlements.remove(entitlement);
    }

    public Set<Entitlement> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(final List<Entitlement> entitlements) {
        this.entitlements.clear();
        if (entitlements != null && !entitlements.isEmpty()) {
            this.entitlements.addAll(entitlements);
        }
    }

    public boolean isInheritTemplates() {
        return isBooleanAsInteger(inheritTemplates);
    }

    public void setInheritTemplates(final boolean inheritAttrTemplates) {
        this.inheritTemplates = getBooleanAsInteger(inheritAttrTemplates);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends AbstractAttrTemplate> List<T> getAttrTemplates(final Class<T> reference) {
        List<T> result = null;

        if (reference.equals(RAttrTemplate.class)) {
            result = (List<T>) rAttrTemplates;
        } else if (reference.equals(RDerAttrTemplate.class)) {
            result = (List<T>) rDerAttrTemplates;
        } else if (reference.equals(RVirAttrTemplate.class)) {
            result = (List<T>) rVirAttrTemplates;
        } else if (reference.equals(MAttrTemplate.class)) {
            result = (List<T>) mAttrTemplates;
        } else if (reference.equals(MDerAttrTemplate.class)) {
            result = (List<T>) mDerAttrTemplates;
        } else if (reference.equals(MVirAttrTemplate.class)) {
            result = (List<T>) mVirAttrTemplates;
        }

        return result;
    }

    public <T extends AbstractAttrTemplate<K>, K extends AbstractSchema> T getAttrTemplate(
            final Class<T> reference, final String schemaName) {

        T result = null;

        for (T template : findInheritedTemplates(reference)) {
            if (schemaName.equals(template.getSchema().getName())) {
                result = template;
            }
        }

        return result;
    }

    public <T extends AbstractAttrTemplate<K>, K extends AbstractSchema> List<K> getAttrTemplateSchemas(
            final Class<T> reference) {

        List<K> result = new ArrayList<K>();

        for (T template : findInheritedTemplates(reference)) {
            result.add(template.getSchema());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractAttrTemplate<K>, K extends AbstractSchema> List<T> findInheritedTemplates(
            final Class<T> reference) {

        List<T> result = new ArrayList<T>(getAttrTemplates(reference));

        if (isInheritTemplates() && getParent() != null) {
            result.addAll(getParent().findInheritedTemplates(reference));
        }

        return result;
    }

    @Override
    public <T extends AbstractAttr> boolean addAttr(final T attr) {
        if (!(attr instanceof RAttr)) {
            throw new ClassCastException("attribute is expected to be typed RAttr: " + attr.getClass().getName());
        }
        return attrs.add((RAttr) attr);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttr(final T attr) {
        if (!(attr instanceof RAttr)) {
            throw new ClassCastException("attribute is expected to be typed RAttr: " + attr.getClass().getName());
        }
        return attrs.remove((RAttr) attr);
    }

    @Override
    public List<? extends AbstractAttr> getAttrs() {
        return attrs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAttrs(final List<? extends AbstractAttr> attrs) {
        this.attrs.clear();
        if (attrs != null && !attrs.isEmpty()) {
            this.attrs.addAll((List<RAttr>) attrs);
        }
    }

    @Override
    public <T extends AbstractDerAttr> boolean addDerAttr(final T derAttr) {
        if (!(derAttr instanceof RDerAttr)) {
            throw new ClassCastException("attribute is expected to be typed RDerAttr: " + derAttr.getClass().getName());
        }
        return derAttrs.add((RDerAttr) derAttr);
    }

    @Override
    public <T extends AbstractDerAttr> boolean removeDerAttr(final T derAttr) {
        if (!(derAttr instanceof RDerAttr)) {
            throw new ClassCastException("attribute is expected to be typed RDerAttr: " + derAttr.getClass().getName());
        }
        return derAttrs.remove((RDerAttr) derAttr);
    }

    @Override
    public List<? extends AbstractDerAttr> getDerAttrs() {
        return derAttrs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setDerAttrs(final List<? extends AbstractDerAttr> derAttrs) {
        this.derAttrs.clear();
        if (derAttrs != null && !derAttrs.isEmpty()) {
            this.derAttrs.addAll((List<RDerAttr>) derAttrs);
        }
    }

    @Override
    public <T extends AbstractVirAttr> boolean addVirAttr(final T virAttr) {
        if (!(virAttr instanceof RVirAttr)) {
            throw new ClassCastException("attribute is expected to be typed RVirAttr: " + virAttr.getClass().getName());
        }
        return virAttrs.add((RVirAttr) virAttr);
    }

    @Override
    public <T extends AbstractVirAttr> boolean removeVirAttr(final T virAttr) {
        if (!(virAttr instanceof RVirAttr)) {
            throw new ClassCastException("attribute is expected to be typed RVirAttr: " + virAttr.getClass().getName());
        }
        return virAttrs.remove((RVirAttr) virAttr);
    }

    @Override
    public List<? extends AbstractVirAttr> getVirAttrs() {
        return virAttrs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setVirAttrs(final List<? extends AbstractVirAttr> virAttrs) {
        this.virAttrs.clear();
        if (virAttrs != null && !virAttrs.isEmpty()) {
            this.virAttrs.addAll((List<RVirAttr>) virAttrs);
        }
    }

    public boolean isInheritAttrs() {
        return isBooleanAsInteger(inheritAttrs);
    }

    public void setInheritAttrs(final boolean inheritAttrs) {
        this.inheritAttrs = getBooleanAsInteger(inheritAttrs);
    }

    /**
     * Get all inherited attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    @SuppressWarnings("unchecked")
    public List<RAttr> findInheritedAttrs() {
        final Map<RSchema, RAttr> result = new HashMap<RSchema, RAttr>();

        if (isInheritAttrs() && getParent() != null) {
            final Map<AbstractNormalSchema, AbstractAttr> attrMap = getAttrMap();

            // Add attributes not specialized
            for (RAttr attr : (Collection<RAttr>) getParent().getAttrs()) {
                if (!attrMap.containsKey(attr.getSchema())) {
                    result.put((RSchema) attr.getSchema(), attr);
                }
            }

            // Add attributes not specialized and not already added
            for (RAttr attr : getParent().findInheritedAttrs()) {
                if (!attrMap.containsKey(attr.getSchema()) && !result.containsKey((RSchema) attr.getSchema())) {
                    result.put((RSchema) attr.getSchema(), attr);
                }
            }
        }

        return new ArrayList<RAttr>(result.values());
    }

    public boolean isInheritDerAttrs() {
        return isBooleanAsInteger(inheritDerAttrs);
    }

    public void setInheritDerAttrs(final boolean inheritDerAttrs) {
        this.inheritDerAttrs = getBooleanAsInteger(inheritDerAttrs);

    }

    /**
     * Get all inherited derived attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    @SuppressWarnings("unchecked")
    public List<RDerAttr> findInheritedDerAttrs() {
        final Map<RDerSchema, RDerAttr> result = new HashMap<RDerSchema, RDerAttr>();

        if (isInheritDerAttrs() && getParent() != null) {
            final Map<AbstractDerSchema, AbstractDerAttr> attrMap = getDerAttrMap();

            // Add attributes not specialized
            for (RDerAttr attr : (Collection<RDerAttr>) getParent().getDerAttrs()) {
                if (!attrMap.containsKey(attr.getSchema())) {
                    result.put((RDerSchema) attr.getSchema(), attr);
                }
            }

            // Add attributes not specialized and not already added
            for (RDerAttr attr : getParent().findInheritedDerAttrs()) {
                if (!attrMap.containsKey(attr.getSchema())
                        && !result.containsKey((RDerSchema) attr.getSchema())) {

                    result.put((RDerSchema) attr.getSchema(), attr);
                }
            }
        }

        return new ArrayList<RDerAttr>(result.values());
    }

    public boolean isInheritVirAttrs() {
        return isBooleanAsInteger(inheritVirAttrs);
    }

    public void setInheritVirAttrs(final boolean inheritVirAttrs) {
        this.inheritVirAttrs = getBooleanAsInteger(inheritVirAttrs);

    }

    /**
     * Get all inherited virtual attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    @SuppressWarnings("unchecked")
    public List<RVirAttr> findInheritedVirAttrs() {
        final Map<RVirSchema, RVirAttr> result = new HashMap<RVirSchema, RVirAttr>();

        if (isInheritVirAttrs() && getParent() != null) {
            final Map<AbstractVirSchema, AbstractVirAttr> attrMap = getVirAttrMap();

            // Add attributes not specialized
            for (RVirAttr attr : (Collection<RVirAttr>) getParent().getVirAttrs()) {
                if (!attrMap.containsKey(attr.getSchema())) {
                    result.put((RVirSchema) attr.getSchema(), attr);
                }
            }

            // Add attributes not specialized and not already added
            for (RVirAttr attr : getParent().findInheritedVirAttrs()) {
                if (!attrMap.containsKey(attr.getSchema())
                        && !result.containsKey((RVirSchema) attr.getSchema())) {

                    result.put((RVirSchema) attr.getSchema(), attr);
                }
            }
        }

        return new ArrayList<RVirAttr>(result.values());
    }

    /**
     * Get first valid password policy.
     *
     * @return parent password policy if isInheritPasswordPolicy is 'true' and parent is not null, local password policy
     * otherwise
     */
    public PasswordPolicy getPasswordPolicy() {
        return isInheritPasswordPolicy() && getParent() != null
                ? getParent().getPasswordPolicy()
                : passwordPolicy;
    }

    public void setPasswordPolicy(final PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public boolean isInheritPasswordPolicy() {
        return isBooleanAsInteger(inheritPasswordPolicy);
    }

    public void setInheritPasswordPolicy(final boolean inheritPasswordPolicy) {
        this.inheritPasswordPolicy = getBooleanAsInteger(inheritPasswordPolicy);
    }

    /**
     * Get first valid account policy.
     *
     * @return parent account policy if isInheritAccountPolicy is 'true' and parent is not null, local account policy
     * otherwise.
     */
    public AccountPolicy getAccountPolicy() {
        return isInheritAccountPolicy() && getParent() != null
                ? getParent().getAccountPolicy()
                : accountPolicy;
    }

    public void setAccountPolicy(final AccountPolicy accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public boolean isInheritAccountPolicy() {
        return isBooleanAsInteger(inheritAccountPolicy);
    }

    public void setInheritAccountPolicy(boolean inheritAccountPolicy) {
        this.inheritAccountPolicy = getBooleanAsInteger(inheritAccountPolicy);
    }
}
