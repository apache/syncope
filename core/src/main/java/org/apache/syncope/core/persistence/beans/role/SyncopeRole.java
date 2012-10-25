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
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirSchema;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.validation.entity.SyncopeRoleCheck;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {"name", "parent_id"}))
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
    private List<RAttr> attributes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RDerAttr> derivedAttributes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RVirAttr> virtualAttributes;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritOwner;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritAttributes;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritDerivedAttributes;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritVirtualAttributes;

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
        attributes = new ArrayList<RAttr>();
        derivedAttributes = new ArrayList<RDerAttr>();
        virtualAttributes = new ArrayList<RVirAttr>();
        inheritOwner = getBooleanAsInteger(false);
        inheritAttributes = getBooleanAsInteger(false);
        inheritDerivedAttributes = getBooleanAsInteger(false);
        inheritVirtualAttributes = getBooleanAsInteger(false);
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

    @Override
    public <T extends AbstractAttr> boolean addAttribute(final T attribute) {
        if (!(attribute instanceof RAttr)) {
            throw new ClassCastException("attribute is expected to be typed RAttr: " + attribute.getClass().getName());
        }
        return attributes.add((RAttr) attribute);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttribute(final T attribute) {
        if (!(attribute instanceof RAttr)) {
            throw new ClassCastException("attribute is expected to be typed RAttr: " + attribute.getClass().getName());
        }
        return attributes.remove((RAttr) attribute);
    }

    @Override
    public List<? extends AbstractAttr> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(final List<? extends AbstractAttr> attributes) {
        this.attributes.clear();
        if (attributes != null && !attributes.isEmpty()) {
            this.attributes.addAll((List<RAttr>) attributes);
        }
    }

    @Override
    public <T extends AbstractDerAttr> boolean addDerivedAttribute(final T derAttr) {
        if (!(derAttr instanceof RDerAttr)) {
            throw new ClassCastException("attribute is expected to be typed RDerAttr: " + derAttr.getClass().getName());
        }
        return derivedAttributes.add((RDerAttr) derAttr);
    }

    @Override
    public <T extends AbstractDerAttr> boolean removeDerivedAttribute(final T derAttr) {
        if (!(derAttr instanceof RDerAttr)) {
            throw new ClassCastException("attribute is expected to be typed RDerAttr: " + derAttr.getClass().getName());
        }
        return derivedAttributes.remove((RDerAttr) derAttr);
    }

    @Override
    public List<? extends AbstractDerAttr> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public void setDerivedAttributes(final List<? extends AbstractDerAttr> derivedAttributes) {
        this.attributes.clear();
        if (attributes != null && !attributes.isEmpty()) {
            this.attributes.addAll((List<RAttr>) attributes);
        }
    }

    @Override
    public <T extends AbstractVirAttr> boolean addVirtualAttribute(final T virAttr) {
        if (!(virAttr instanceof RVirAttr)) {
            throw new ClassCastException("attribute is expected to be typed RVirAttr: " + virAttr.getClass().getName());
        }
        return virtualAttributes.add((RVirAttr) virAttr);
    }

    @Override
    public <T extends AbstractVirAttr> boolean removeVirtualAttribute(final T virAttr) {
        if (!(virAttr instanceof RVirAttr)) {
            throw new ClassCastException("attribute is expected to be typed RVirAttr: " + virAttr.getClass().getName());
        }
        return virtualAttributes.remove((RVirAttr) virAttr);
    }

    @Override
    public List<? extends AbstractVirAttr> getVirtualAttributes() {
        return virtualAttributes;
    }

    @Override
    public void setVirtualAttributes(final List<? extends AbstractVirAttr> virtualAttributes) {
        this.virtualAttributes.clear();
        if (virtualAttributes != null && !virtualAttributes.isEmpty()) {
            this.virtualAttributes.addAll((List<RVirAttr>) virtualAttributes);
        }
    }

    public boolean isInheritAttributes() {
        return isBooleanAsInteger(inheritAttributes);
    }

    public void setInheritAttributes(final boolean inheritAttributes) {
        this.inheritAttributes = getBooleanAsInteger(inheritAttributes);
    }

    /**
     * Get all inherited attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    public List<RAttr> findInheritedAttributes() {
        final Map<RSchema, RAttr> result = new HashMap<RSchema, RAttr>();

        if (isInheritAttributes() && getParent() != null) {
            final Map<AbstractSchema, AbstractAttr> attrMap = getAttrMap();

            // Add attributes not specialized
            for (RAttr attr : (Collection<RAttr>) getParent().getAttributes()) {
                if (!attrMap.containsKey(attr.getSchema())) {
                    result.put((RSchema) attr.getSchema(), attr);
                }
            }

            // Add attributes not specialized and not already added
            for (RAttr attr : getParent().findInheritedAttributes()) {
                if (!attrMap.containsKey(attr.getSchema()) && !result.containsKey((RSchema) attr.getSchema())) {
                    result.put((RSchema) attr.getSchema(), attr);
                }
            }
        }

        return new ArrayList<RAttr>(result.values());
    }

    public boolean isInheritDerivedAttributes() {
        return isBooleanAsInteger(inheritDerivedAttributes);
    }

    public void setInheritDerivedAttributes(final boolean inheritDerivedAttributes) {
        this.inheritDerivedAttributes = getBooleanAsInteger(inheritDerivedAttributes);

    }

    /**
     * Get all inherited derived attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    public List<RDerAttr> findInheritedDerivedAttributes() {
        final Map<RDerSchema, RDerAttr> result = new HashMap<RDerSchema, RDerAttr>();

        if (isInheritDerivedAttributes() && getParent() != null) {
            final Map<AbstractDerSchema, AbstractDerAttr> attrMap = getDerAttrMap();

            // Add attributes not specialized
            for (RDerAttr attr : (Collection<RDerAttr>) getParent().getDerivedAttributes()) {
                if (!attrMap.containsKey(attr.getDerivedSchema())) {
                    result.put((RDerSchema) attr.getDerivedSchema(), attr);
                }
            }

            // Add attributes not specialized and not already added
            for (RDerAttr attr : getParent().findInheritedDerivedAttributes()) {
                if (!attrMap.containsKey(attr.getDerivedSchema())
                        && !result.containsKey((RDerSchema) attr.getDerivedSchema())) {
                    result.put((RDerSchema) attr.getDerivedSchema(), attr);
                }
            }
        }

        return new ArrayList<RDerAttr>(result.values());
    }

    public boolean isInheritVirtualAttributes() {
        return isBooleanAsInteger(inheritVirtualAttributes);
    }

    public void setInheritVirtualAttributes(final boolean inheritVirtualAttributes) {
        this.inheritVirtualAttributes = getBooleanAsInteger(inheritVirtualAttributes);

    }

    /**
     * Get all inherited virtual attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    public List<RVirAttr> findInheritedVirtualAttributes() {
        final Map<RVirSchema, RVirAttr> result = new HashMap<RVirSchema, RVirAttr>();

        if (isInheritVirtualAttributes() && getParent() != null) {
            final Map<AbstractVirSchema, AbstractVirAttr> attrMap = getVirAttrMap();

            // Add attributes not specialized
            for (RVirAttr attr : (Collection<RVirAttr>) getParent().getVirtualAttributes()) {
                if (!attrMap.containsKey(attr.getVirtualSchema())) {
                    result.put((RVirSchema) attr.getVirtualSchema(), attr);
                }
            }

            // Add attributes not specialized and not already added
            for (RVirAttr attr : getParent().findInheritedVirtualAttributes()) {
                if (!attrMap.containsKey(attr.getVirtualSchema())
                        && !result.containsKey((RVirSchema) attr.getVirtualSchema())) {
                    result.put((RVirSchema) attr.getVirtualSchema(), attr);
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
