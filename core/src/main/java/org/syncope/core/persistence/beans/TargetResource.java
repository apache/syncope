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
import javax.persistence.OneToMany;
import javax.validation.Valid;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.validator.constraints.Range;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.validation.entity.TargetResourceCheck;
import org.syncope.types.SourceMappingType;

/**
 * A resource to which propagation occurs.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@TargetResourceCheck
public class TargetResource extends AbstractBaseBean {

    /**
     * The resource identifier is the name.
     */
    @Id
    private String name;

    /**
     * Should this resource enforce the mandatory constraints?
     */
    @Column(nullable = false)
    @Basic
    @Range(min = 0, max = 1)
    private Integer forceMandatoryConstraint;

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
    @Valid
    private List<SchemaMapping> mappings;

    /**
     * Tasks associated to this resource.
     */
    @OneToMany(cascade = CascadeType.MERGE, mappedBy = "resource")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @Valid
    private List<Task> tasks;

    /**
     * Default constructor.
     */
    public TargetResource() {
        super();

        forceMandatoryConstraint = getBooleanAsInteger(false);
        users = new HashSet<SyncopeUser>();
        roles = new HashSet<SyncopeRole>();
        mappings = new ArrayList<SchemaMapping>();
        tasks = new ArrayList<Task>();
    }

    public boolean isForceMandatoryConstraint() {
        return isBooleanAsInteger(forceMandatoryConstraint);
    }

    public void setForceMandatoryConstraint(boolean forceMandatoryConstraint) {
        this.forceMandatoryConstraint =
                getBooleanAsInteger(forceMandatoryConstraint);
    }

    public ConnectorInstance getConnector() {
        return connector;
    }

    public void setConnector(ConnectorInstance connector) {
        this.connector = connector;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public boolean addTask(Task task) {
        return this.tasks.add(task);
    }

    public boolean removeTask(Task task) {
        return this.tasks.remove(task);
    }

    public void setTasks(List<Task> tasks) {
        this.tasks.clear();
        if (tasks != null && !tasks.isEmpty()) {
            this.tasks.addAll(tasks);
        }
    }

    public List<SchemaMapping> getMappings() {
        return mappings;
    }

    public List<SchemaMapping> getMappings(final String sourceAttrName,
            final SourceMappingType sourceMappingType) {

        List<SchemaMapping> result = new ArrayList<SchemaMapping>();
        for (SchemaMapping mapping : mappings) {
            if (mapping.getSourceAttrName().equals(sourceAttrName)
                    && mapping.getSourceMappingType() == sourceMappingType) {

                result.add(mapping);
            }
        }

        return result;
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

    public boolean addRole(SyncopeRole role) {
        return roles.add(role);
    }

    public boolean removeRole(SyncopeRole role) {
        return roles.remove(role);
    }

    public Set<SyncopeRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<SyncopeRole> roles) {
        this.roles.clear();
        if (roles != null && !roles.isEmpty()) {
            this.roles.addAll(roles);
        }
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
        this.users.clear();
        if (users != null && !users.isEmpty()) {
            this.users.addAll(users);
        }
    }
}
