/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.persistence.beans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.validation.entity.SyncTaskCheck;
import org.syncope.core.scheduling.SyncJob;

@Entity
@SyncTaskCheck
public class SyncTask extends SchedTask {

    private static final long serialVersionUID = -4141057723006682562L;

    /**
     * ExternalResource to which the sync happens.
     */
    @ManyToOne
    private ExternalResource resource;

    @OneToMany
    private List<ExternalResource> defaultResources;

    @OneToMany
    private List<SyncopeRole> defaultRoles;

    @Basic
    @Min(0)
    @Max(1)
    private Integer performCreate;

    @Basic
    @Min(0)
    @Max(1)
    private Integer performUpdate;

    @Basic
    @Min(0)
    @Max(1)
    private Integer performDelete;

    private String jobActionsClassName;

    /**
     * Default constructor.
     */
    public SyncTask() {
        super();

        defaultResources = new ArrayList<ExternalResource>();
        defaultRoles = new ArrayList<SyncopeRole>();
        super.setJobClassName(SyncJob.class.getName());
    }

    @Override
    public void setJobClassName(String jobClassName) {
    }

    public ExternalResource getResource() {
        return resource;
    }

    public void setResource(ExternalResource resource) {
        this.resource = resource;
    }

    public boolean addDefaultResource(ExternalResource resource) {
        return resource != null && !defaultResources.contains(resource)
                && defaultResources.add(resource);
    }

    public boolean removeDefaultResource(ExternalResource resource) {
        return resource != null && defaultResources.remove(resource);
    }

    public Set<String> getDefaultResourceNames() {
        Set<String> defaultResourceNames = new HashSet<String>(
                getDefaultResources().size());
        for (ExternalResource defaultResource : getDefaultResources()) {
            defaultResourceNames.add(defaultResource.getName());
        }
        return defaultResourceNames;
    }

    public List<ExternalResource> getDefaultResources() {
        return defaultResources;
    }

    public void setDefaultResources(List<ExternalResource> defaultResources) {
        this.defaultResources.clear();
        if (defaultResources != null && !defaultResources.isEmpty()) {
            this.defaultResources.addAll(defaultResources);
        }
    }

    public boolean addDefaultRole(SyncopeRole role) {
        return role != null && !defaultRoles.contains(role)
                && defaultRoles.add(role);
    }

    public boolean removeDefaultRole(SyncopeRole role) {
        return role != null && defaultRoles.remove(role);
    }

    public Set<Long> getDefaultRoleIds() {
        Set<Long> defaultRoleIds = new HashSet<Long>(
                getDefaultRoles().size());
        for (SyncopeRole defaultRole : getDefaultRoles()) {
            defaultRoleIds.add(defaultRole.getId());
        }
        return defaultRoleIds;
    }

    public List<SyncopeRole> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<SyncopeRole> defaultRoles) {
        this.defaultRoles.clear();
        if (defaultRoles != null && !defaultRoles.isEmpty()) {
            this.defaultRoles.addAll(defaultRoles);
        }
    }

    public boolean isPerformCreate() {
        return isBooleanAsInteger(performCreate);
    }

    public void setPerformCreate(final boolean performCreate) {
        this.performCreate = getBooleanAsInteger(performCreate);
    }

    public boolean isPerformUpdate() {
        return isBooleanAsInteger(performUpdate);
    }

    public void setPerformUpdate(final boolean performUpdate) {
        this.performUpdate = getBooleanAsInteger(performUpdate);
    }

    public boolean isPerformDelete() {
        return isBooleanAsInteger(performDelete);
    }

    public void setPerformDelete(boolean performDelete) {
        this.performDelete = getBooleanAsInteger(performDelete);
    }

    public String getJobActionsClassName() {
        return jobActionsClassName;
    }

    public void setJobActionsClassName(String jobActionsClassName) {
        this.jobActionsClassName = jobActionsClassName;
    }
}
