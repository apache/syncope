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

public class ConnectorInstanceTOs extends AbstractBaseTO
        implements Iterable<ConnectorInstanceTO> {

    private List<ConnectorInstanceTO> instances;

    public ConnectorInstanceTOs() {
        instances = new ArrayList<ConnectorInstanceTO>();
    }

    public List<ConnectorInstanceTO> getInstances() {
        return instances;
    }

    public void setInstances(List<ConnectorInstanceTO> instances) {
        this.instances = instances;
    }

    public boolean addInstance(ConnectorInstanceTO instance) {
        if (this.instances == null) {
            this.instances = new ArrayList<ConnectorInstanceTO>();
        }

        return this.instances.add(instance);
    }

    public boolean removeInstance(ConnectorInstanceTO instance) {
        if (this.instances == null) return true;

        return this.instances.remove(instance);
    }

    @Override
    public Iterator<ConnectorInstanceTO> iterator() {
        if (this.instances == null) {
            this.instances = new ArrayList<ConnectorInstanceTO>();
        }

        return this.instances.iterator();
    }
}
