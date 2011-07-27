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
package org.syncope.client.mod;

import java.util.ArrayList;
import java.util.List;

public class SyncTaskMod extends SchedTaskMod {

    private static final long serialVersionUID = 2194093947655403368L;

    private List<String> defaultResources;

    private List<Long> defaultRoles;

    private boolean updateIdentities;

    public SyncTaskMod() {
        super();

        defaultResources = new ArrayList<String>();
        defaultRoles = new ArrayList<Long>();
    }

    public List<String> getDefaultResources() {
        return defaultResources;
    }

    public void setDefaultResources(List<String> defaultResources) {
        this.defaultResources = defaultResources;
    }

    public List<Long> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<Long> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public boolean isUpdateIdentities() {
        return updateIdentities;
    }

    public void setUpdateIdentities(boolean updateIdentities) {
        this.updateIdentities = updateIdentities;
    }
}
