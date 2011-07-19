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
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.validation.entity.SchedTaskCheck;
import org.syncope.core.scheduling.SyncJob;

@Entity
@SchedTaskCheck
public class SyncTask extends SchedTask {

    private static final long serialVersionUID = -4141057723006682562L;

    /**
     * TargetResource to which the sync happens.
     */
    @ManyToOne(optional = false)
    private TargetResource resource;

    @OneToMany
    private List<TargetResource> defaultResources;

    @OneToMany
    private List<SyncopeRole> defaultRoles;

    @Basic
    @Min(0)
    @Max(1)
    private Integer updateIdentities;

    /**
     * Default constructor.
     */
    public SyncTask() {
        super();

        defaultResources = new ArrayList<TargetResource>();
        defaultRoles = new ArrayList<SyncopeRole>();
        super.setJobClassName(SyncJob.class.getName());
    }

    @Override
    public void setJobClassName(String jobClassName) {
    }

    public TargetResource getResource() {
        return resource;
    }

    public void setResource(TargetResource resource) {
        this.resource = resource;
    }

    public boolean addDefaultResource(TargetResource resource) {
        return resource != null && !defaultResources.contains(resource)
                && defaultResources.add(resource);
    }

    public boolean removeDefaultResource(TargetResource resource) {
        return resource != null && defaultResources.remove(resource);
    }

    public List<TargetResource> getDefaultResources() {
        return defaultResources;
    }

    public void setDefaultResources(List<TargetResource> defaultResources) {
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

    public List<SyncopeRole> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<SyncopeRole> defaultRoles) {
        this.defaultRoles.clear();
        if (defaultRoles != null && !defaultRoles.isEmpty()) {
            this.defaultRoles.addAll(defaultRoles);
        }
    }

    public boolean isUpdateIdentities() {
        return isBooleanAsInteger(updateIdentities);
    }

    public void setUpdateIdentities(boolean updateIdentities) {
        this.updateIdentities = getBooleanAsInteger(updateIdentities);
    }
}
