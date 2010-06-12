/*
 *  Copyright 2010 ilgrosso.
 * 
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
package org.syncope.core.persistence.beans;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;

@Entity
public class SyncopeRole extends AbstractAttributableBean {

    @EmbeddedId
    private SyncopeRolePK syncopeRolePK;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
    mappedBy = "roles")
    private Set<SyncopeUser> users;
    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Entitlement> entitlements;

    public SyncopeRole() {
        users = new HashSet<SyncopeUser>();
        entitlements = new HashSet<Entitlement>();
    }

    public SyncopeRolePK getSyncopeRolePK() {
        return syncopeRolePK;
    }

    public void setSyncopeRolePK(SyncopeRolePK syncopeRolePK) {
        this.syncopeRolePK = syncopeRolePK;
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

    public Set<SyncopeUser> getUsers() {
        return users;
    }

    public void setUsers(Set<SyncopeUser> users) {
        this.users = users;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SyncopeRole other = (SyncopeRole) obj;
        if (this.syncopeRolePK != other.syncopeRolePK
                && (this.syncopeRolePK == null
                || !this.syncopeRolePK.equals(other.syncopeRolePK))) {

            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (this.syncopeRolePK != null
                ? this.syncopeRolePK.hashCode() : 0);
        return hash;
    }
}
