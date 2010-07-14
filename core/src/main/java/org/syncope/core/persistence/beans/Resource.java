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
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;

@Entity
public class Resource extends AbstractBaseBean {

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
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<SyncopeUser> users;

    /**
     * Roles associated to this resource.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<SyncopeRole> roles;

    /**
     * Attribute mappings.
     */
    @OneToMany(cascade = {CascadeType.REFRESH, CascadeType.MERGE},
    fetch = FetchType.EAGER, mappedBy = "resource")
    private Set<SchemaMapping> mappings;

    public ConnectorInstance getConnector() {
        return connector;
    }

    public void setConnector(ConnectorInstance connector) {
        this.connector = connector;
    }

    public Set<SchemaMapping> getMappings() {
        if (this.mappings == null) this.mappings = new HashSet<SchemaMapping>();
        return this.mappings;
    }

    public boolean removeMapping(SchemaMapping mapping) {
        if (this.mappings == null) return true;
        return this.mappings.remove(mapping);
    }

    public boolean addMapping(SchemaMapping mapping) {
        if (this.mappings == null) this.mappings = new HashSet<SchemaMapping>();
        return this.mappings.add(mapping);
    }

    public void setMappings(Set<SchemaMapping> mappings) {
        this.mappings = mappings;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<SyncopeRole> getRoles() {
        if (this.roles == null) this.roles = new HashSet<SyncopeRole>();
        return this.roles;
    }

    public boolean addRole(SyncopeRole role) {
        if (this.roles == null) this.roles = new HashSet<SyncopeRole>();
        return this.roles.add(role);
    }

    public boolean removeRole(SyncopeRole role) {
        if (this.roles == null) return true;
        return this.roles.remove(role);
    }

    public void setRoles(Set<SyncopeRole> roles) {
        this.roles = roles;
    }

    public Set<SyncopeUser> getUsers() {
        if (this.users == null) this.users = new HashSet<SyncopeUser>();
        return this.users;
    }

    public boolean addUser(SyncopeUser user) {
        if (this.users == null) this.users = new HashSet<SyncopeUser>();
        return this.users.add(user);
    }

    public boolean removeUser(SyncopeUser user) {
        if (this.users == null) return true;
        return this.users.remove(user);
    }

    public void setUsers(Set<SyncopeUser> users) {
        this.users = users;
    }
}
