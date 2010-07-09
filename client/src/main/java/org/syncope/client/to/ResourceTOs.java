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
import java.util.Iterator;
import java.util.List;

public class ResourceTOs extends AbstractBaseTO
        implements Iterable<ResourceTO> {

    private List<ResourceTO> resources;

    public List<ResourceTO> getResources() {
        if (this.resources == null)
            this.resources = new ArrayList<ResourceTO>();
        return this.resources;
    }

    public boolean addResource(ResourceTO resource) {
        if (this.resources == null)
            this.resources = new ArrayList<ResourceTO>();
        return this.resources.add(resource);
    }

    public boolean removeResource(ResourceTO resource) {
        if (this.resources == null) return true;
        return this.resources.remove(resource);
    }

    public void setResources(List<ResourceTO> resources) {
        this.resources = resources;
    }

    @Override
    public Iterator<ResourceTO> iterator() {
        if (this.resources == null) {
            this.resources = new ArrayList<ResourceTO>();
        }

        return this.resources.iterator();
    }
}
