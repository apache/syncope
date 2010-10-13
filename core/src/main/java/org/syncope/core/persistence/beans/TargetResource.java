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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@NamedQueries({
    @NamedQuery(name = "TargetResource.find",
    query = "SELECT e FROM TargetResource e WHERE e.name = :name",
    hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true")
    }),
    @NamedQuery(name = "TargetResource.getMappings",
    query = "SELECT m FROM SchemaMapping m WHERE m.schemaName=:schemaName "
    + "AND m.schemaType=:schemaType",
    hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheMode", value = "refresh")
    }),
    @NamedQuery(name = "TargetResource.getMappingsByTargetResource",
    query = "SELECT m FROM SchemaMapping m WHERE m.schemaName=:schemaName "
    + "AND m.schemaType=:schemaType AND m.resource.name=:resourceName",
    hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheMode", value = "refresh")
    })
})
public class TargetResource extends AbstractBaseBean {

    /**
     * The resource identifier is the name.
     */
    @Id
    private String name;
    /**
     * The resource type is identified by the associated connector.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private ConnectorInstance connector;
    /**
     * Users associated to this resource.
     */
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "targetResources")
    private Set<SyncopeUser> users;
    /**
     * Roles associated to this resource.
     */
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "targetResources")
    private Set<SyncopeRole> roles;
    /**
     * Attribute mappings.
     */
    @OneToMany(cascade = CascadeType.MERGE, mappedBy = "resource")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<SchemaMapping> mappings;
    @Column(nullable = false)
    @Basic
    private Character forceMandatoryConstraint;

    public TargetResource() {
        forceMandatoryConstraint = getBooleanAsCharacter(false);
        mappings = new ArrayList<SchemaMapping>();
    }

    public boolean isForceMandatoryConstraint() {
        return isBooleanAsCharacter(forceMandatoryConstraint);
    }

    public void setForceMandatoryConstraint(boolean forceMandatoryConstraint) {
        this.forceMandatoryConstraint =
                getBooleanAsCharacter(forceMandatoryConstraint);
    }

    public ConnectorInstance getConnector() {
        return connector;
    }

    public void setConnector(ConnectorInstance connector) {
        this.connector = connector;
    }

    public List<SchemaMapping> getMappings() {
        return mappings;
    }

    public boolean removeMapping(SchemaMapping mapping) {
        return mappings == null || mappings.remove(mapping);
    }

    public boolean addMapping(SchemaMapping mapping) {
        return mappings.contains(mapping) || mappings.add(mapping);
    }

    public void setMappings(List<SchemaMapping> mappings) {
        this.mappings.clear();
        if (mappings != null) {
            this.mappings.addAll(mappings);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<SyncopeRole> getRoles() {
        if (this.roles == null) {
            this.roles = new HashSet<SyncopeRole>();
        }
        return this.roles;
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

    public void setRoles(Set<SyncopeRole> roles) {
        this.roles = roles;
    }

    public Set<SyncopeUser> getUsers() {
        if (this.users == null) {
            this.users = new HashSet<SyncopeUser>();
        }
        return this.users;
    }

    public boolean addUser(SyncopeUser user) {
        if (this.users == null) {
            this.users = new HashSet<SyncopeUser>();
        }
        return this.users.add(user);
    }

    public boolean removeUser(SyncopeUser user) {
        if (this.users == null) {
            return true;
        }
        return this.users.remove(user);
    }

    public void setUsers(Set<SyncopeUser> users) {
        this.users = users;
    }
}
