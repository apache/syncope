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
package org.apache.syncope.core.persistence.jpa.entity.role;

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
import org.apache.syncope.core.persistence.api.entity.role.RDerAttr;
import org.apache.syncope.core.persistence.api.entity.role.RDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.role.RDerSchema;
import org.apache.syncope.core.persistence.api.entity.role.RPlainAttr;
import org.apache.syncope.core.persistence.api.entity.role.RPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.role.RVirAttr;
import org.apache.syncope.core.persistence.api.entity.role.RVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.role.RVirSchema;
import org.apache.syncope.core.persistence.api.entity.role.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.validation.entity.RoleCheck;
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
@Table(name = JPARole.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "name", "parent_id" }))
@Cacheable
@RoleCheck
public class JPARole extends AbstractSubject<RPlainAttr, RDerAttr, RVirAttr> implements Role {

    private static final long serialVersionUID = -5281258853142421875L;

    public static final String TABLE = "SyncopeRole";

    @Id
    private Long id;

    @NotNull
    private String name;

    @ManyToOne(optional = true)
    private JPARole parent;

    @ManyToOne(optional = true)
    private JPAUser userOwner;

    @ManyToOne(optional = true)
    private JPARole roleOwner;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "entitlement_name"))
    private Set<JPAEntitlement> entitlements;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPARPlainAttrTemplate> rAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPARDerAttrTemplate> rDerAttrTemplates;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPARVirAttrTemplate> rVirAttrTemplates;

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
    private List<JPARPlainAttr> plainAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPARDerAttr> derAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPARVirAttr> virAttrs;

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
            @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_name"))
    @Valid
    private Set<JPAExternalResource> resources;

    public JPARole() {
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
    public Role getParent() {
        return parent;
    }

    @Override
    public void setParent(final Role parent) {
        checkType(parent, JPARole.class);
        this.parent = (JPARole) parent;
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
    public JPARole getRoleOwner() {
        return roleOwner;
    }

    @Override
    public void setRoleOwner(final Role roleOwner) {
        checkType(roleOwner, JPARole.class);
        this.roleOwner = (JPARole) roleOwner;
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

        if (RPlainAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) rAttrTemplates;
        } else if (RDerAttrTemplate.class.isAssignableFrom(reference)) {
            result = (List<T>) rDerAttrTemplates;
        } else if (RVirAttrTemplate.class.isAssignableFrom(reference)) {
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

        T result = null;

        for (T template : findInheritedTemplates(reference)) {
            if (schemaName.equals(template.getSchema().getKey())) {
                result = template;
            }
        }

        return result;
    }

    @Override
    public <T extends AttrTemplate<K>, K extends Schema> List<K> getAttrTemplateSchemas(final Class<T> reference) {
        final List<K> result = new ArrayList<>();

        for (T template : findInheritedTemplates(reference)) {
            result.add(template.getSchema());
        }

        return result;
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
    public boolean addPlainAttr(final RPlainAttr attr) {
        checkType(attr, JPARPlainAttr.class);
        return plainAttrs.add((JPARPlainAttr) attr);
    }

    @Override
    public boolean removePlainAttr(final RPlainAttr attr) {
        checkType(attr, JPARPlainAttr.class);
        return plainAttrs.remove((JPARPlainAttr) attr);
    }

    @Override
    public List<? extends RPlainAttr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    public boolean addDerAttr(final RDerAttr attr) {
        checkType(attr, JPARDerAttr.class);
        return derAttrs.add((JPARDerAttr) attr);
    }

    @Override
    public boolean removeDerAttr(final RDerAttr attr) {
        checkType(attr, JPARDerAttr.class);
        return derAttrs.remove((JPARDerAttr) attr);
    }

    @Override
    public List<? extends RDerAttr> getDerAttrs() {
        return derAttrs;
    }

    @Override
    public boolean addVirAttr(final RVirAttr attr) {
        checkType(attr, JPARVirAttr.class);
        return virAttrs.add((JPARVirAttr) attr);
    }

    @Override
    public boolean removeVirAttr(final RVirAttr attr) {
        checkType(attr, JPARVirAttr.class);
        return virAttrs.remove((JPARVirAttr) attr);
    }

    @Override
    public List<? extends RVirAttr> getVirAttrs() {
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
    public List<? extends RPlainAttr> findLastInheritedAncestorPlainAttrs() {
        final Map<JPARPlainSchema, RPlainAttr> result = new HashMap<>();

        if (!isInheritPlainAttrs()) {
            return plainAttrs;
        }
        if (isInheritPlainAttrs() && getParent() != null) {
            final Map<PlainSchema, RPlainAttr> attrMap = getPlainAttrMap();

            // Add inherit attributes
            for (RPlainAttr attr : getParent().findLastInheritedAncestorPlainAttrs()) {
                if (attrMap.containsKey(attr.getSchema())) {
                    result.remove((JPARPlainSchema) attr.getSchema());
                }
                result.put((JPARPlainSchema) attr.getSchema(), attr);
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
    public List<? extends RDerAttr> findLastInheritedAncestorDerAttrs() {
        final Map<RDerSchema, RDerAttr> result = new HashMap<>();

        if (!isInheritDerAttrs()) {
            return derAttrs;
        }
        if (isInheritDerAttrs() && getParent() != null) {
            Map<DerSchema, RDerAttr> derAttrMap = getDerAttrMap();

            // Add inherit derived attributes
            for (RDerAttr attr : getParent().findLastInheritedAncestorDerAttrs()) {
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
    public List<? extends RVirAttr> findLastInheritedAncestorVirAttrs() {
        final Map<RVirSchema, RVirAttr> result = new HashMap<>();

        if (!isInheritVirAttrs()) {
            return virAttrs;
        }

        if (isInheritVirAttrs() && getParent() != null) {
            Map<VirSchema, RVirAttr> virAttrMap = getVirAttrMap();

            // Add inherit virtual attributes
            for (RVirAttr attr : getParent().findLastInheritedAncestorVirAttrs()) {
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
