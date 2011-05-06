/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.beans.role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.Entitlement;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {
    "name",
    "parent_id"
}))
@Cacheable
public class SyncopeRole extends AbstractAttributable {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = true)
    private SyncopeRole parent;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Entitlement> entitlements;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RAttr> attributes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RDerAttr> derivedAttributes;

    @Basic
    @Min(0)
    @Max(1)
    private Integer inheritAttributes;

    @Basic
    @Min(0)
    @Max(1)
    private Integer inheritDerivedAttributes;

    public SyncopeRole() {
        super();

        entitlements = new HashSet<Entitlement>();
        attributes = new ArrayList<RAttr>();
        derivedAttributes = new ArrayList<RDerAttr>();
        inheritAttributes = getBooleanAsInteger(false);
        inheritDerivedAttributes = getBooleanAsInteger(false);
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SyncopeRole getParent() {
        return parent;
    }

    public void setParent(SyncopeRole parent) {
        this.parent = parent;
    }

    public boolean addEntitlement(Entitlement entitlement) {
        return entitlements.add(entitlement);
    }

    public boolean removeEntitlement(Entitlement entitlement) {
        return entitlements.remove(entitlement);
    }

    public Set<Entitlement> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(Set<Entitlement> entitlements) {
        this.entitlements = entitlements;
    }

    @Override
    public <T extends AbstractAttr> boolean addAttribute(T attribute) {
        return attributes.add((RAttr) attribute);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttribute(T attribute) {
        return attributes.remove((RAttr) attribute);
    }

    @Override
    public List<? extends AbstractAttr> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(List<? extends AbstractAttr> attributes) {
        this.attributes = (List<RAttr>) attributes;
    }

    @Override
    public <T extends AbstractDerAttr> boolean addDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.add((RDerAttr) derivedAttribute);
    }

    @Override
    public <T extends AbstractDerAttr> boolean removeDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.remove(
                (RDerAttr) derivedAttribute);
    }

    @Override
    public List<? extends AbstractDerAttr> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public void setDerivedAttributes(
            List<? extends AbstractDerAttr> derivedAttributes) {

        this.derivedAttributes = (List<RDerAttr>) derivedAttributes;
    }

    public boolean isInheritAttributes() {
        return isBooleanAsInteger(inheritAttributes);
    }

    public void setInheritAttributes(boolean inheritAttributes) {
        this.inheritAttributes = getBooleanAsInteger(inheritAttributes);
    }

    public List<RAttr> findInheritedAttributes() {
        List<RAttr> result = new ArrayList<RAttr>(attributes);
        if (isInheritAttributes() && getParent() != null) {
            result.addAll(getParent().findInheritedAttributes());
        }

        return result;
    }

    public boolean isInheritDerivedAttributes() {
        return isBooleanAsInteger(inheritDerivedAttributes);
    }

    public void setInheritDerivedAttributes(boolean inheritDerivedAttributes) {
        this.inheritDerivedAttributes =
                getBooleanAsInteger(inheritDerivedAttributes);

    }

    public List<RDerAttr> findInheritedDerivedAttributes() {
        List<RDerAttr> result = new ArrayList<RDerAttr>(derivedAttributes);
        if (isInheritDerivedAttributes() && getParent() != null) {
            result.addAll(getParent().findInheritedDerivedAttributes());
        }

        return result;
    }
}
