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
package org.syncope.core.persistence.beans;

import java.util.HashSet;
import java.util.Set;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class Entitlement extends AbstractBaseBean {

    @Id
    private String name;

    @Column(nullable = true)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "entitlements")
    private Set<SyncopeRole> roles;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean addRole(SyncopeRole role) {
        if (this.roles == null) {
            this.roles = new HashSet<SyncopeRole>();
        }
        return this.roles.add(role);
    }

    public boolean removeRole(SyncopeRole role) {
        if (this.roles == null) {
            return true;
        }
        return this.roles.remove(role);
    }

    public Set<SyncopeRole> getRoles() {
        if (this.roles == null) {
            this.roles = new HashSet<SyncopeRole>();
        }
        return this.roles;
    }

    public void setRoles(Set<SyncopeRole> roles) {
        this.roles = roles;
    }
}
