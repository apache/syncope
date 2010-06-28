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

import org.syncope.core.persistence.beans.user.SyncopeUser;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.Entitlement;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {"name", "parent"}))
public class SyncopeRole extends AbstractAttributable {

    private String name;
    private String parent;
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "roles")
    private Set<SyncopeUser> users;
    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Entitlement> entitlements;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER, mappedBy = "owner")
    private Set<RoleAttribute> attributes;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER, mappedBy = "owner")
    private Set<RoleDerivedAttribute> derivedAttributes;

    public SyncopeRole() {
        users = new HashSet<SyncopeUser>();
        entitlements = new HashSet<Entitlement>();
        attributes = new HashSet<RoleAttribute>();
        derivedAttributes = new HashSet<RoleDerivedAttribute>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws IllegalArgumentException {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
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

    public boolean addUser(SyncopeUser user) {
        return users.add(user);
    }

    public boolean removeUser(SyncopeUser user) {
        return users.remove(user);
    }

    public Set<SyncopeUser> getUsers() {
        return users;
    }

    public void setUsers(Set<SyncopeUser> users) {
        this.users = users;
    }

    @Override
    public <T extends AbstractAttribute> boolean addAttribute(T attribute) {
        return attributes.add((RoleAttribute) attribute);
    }

    @Override
    public <T extends AbstractAttribute> boolean removeAttribute(T attribute) {
        return attributes.remove((RoleAttribute) attribute);
    }

    @Override
    public Set<? extends AbstractAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Set<? extends AbstractAttribute> attributes) {
        this.attributes = (Set<RoleAttribute>) attributes;
    }

    @Override
    public <T extends AbstractDerivedAttribute> boolean addDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.add((RoleDerivedAttribute) derivedAttribute);
    }

    @Override
    public <T extends AbstractDerivedAttribute> boolean removeDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.remove((RoleDerivedAttribute) derivedAttribute);
    }

    @Override
    public Set<? extends AbstractDerivedAttribute> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public void setDerivedAttributes(
            Set<? extends AbstractDerivedAttribute> derivedAttributes) {

        this.derivedAttributes = (Set<RoleDerivedAttribute>) derivedAttributes;
    }
}
