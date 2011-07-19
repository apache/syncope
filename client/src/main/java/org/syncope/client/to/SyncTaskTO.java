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
package org.syncope.client.to;

import java.util.ArrayList;
import java.util.List;

public class SyncTaskTO extends TaskTO {

    private String resource;

    private List<String> defaultResources;

    private List<Long> defaultRoles;

    private boolean updateIdentities;

    public SyncTaskTO() {
        super();

        defaultResources = new ArrayList<String>();
        defaultRoles = new ArrayList<Long>();
    }

    public boolean addDefaultResource(String resource) {
        return resource != null && !defaultResources.contains(resource)
                && defaultResources.add(resource);
    }

    public boolean removeDefaultResource(String resource) {
        return resource != null && defaultResources.remove(resource);
    }

    public List<String> getDefaultResources() {
        return defaultResources;
    }

    public void setDefaultResources(List<String> defaultResources) {
        this.defaultResources = defaultResources;
    }

    public boolean addDefaultRole(Long role) {
        return role != null && !defaultRoles.contains(role)
                && defaultRoles.add(role);
    }

    public boolean removeDefaultRole(Long role) {
        return role != null && defaultRoles.remove(role);
    }

    public List<Long> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<Long> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public boolean isUpdateIdentities() {
        return updateIdentities;
    }

    public void setUpdateIdentities(boolean updateIdentities) {
        this.updateIdentities = updateIdentities;
    }
}
