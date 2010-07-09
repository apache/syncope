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

import java.util.HashSet;
import java.util.Set;

public class ConnectorInstanceTO extends AbstractBaseTO {

    private Long id;

    private String bundleName;

    private String version;

    private String connectorName;

    private Set<PropertyTO> configuration;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundlename) {
        this.bundleName = bundlename;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String bundleversion) {
        this.version = bundleversion;
    }

    public Set<PropertyTO> getConfiguration() {
        if (this.configuration == null)
            this.configuration = new HashSet<PropertyTO>();
        return this.configuration;
    }

    public boolean addConfiguration(PropertyTO property) {
        if (this.configuration == null)
            this.configuration = new HashSet<PropertyTO>();
        return this.configuration.add(property);
    }

    public boolean removeConfiguration(PropertyTO property) {
        if (this.configuration == null) return true;
        return this.configuration.remove(property);
    }

    public void setConfiguration(Set<PropertyTO> configuration) {
        this.configuration = configuration;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorname) {
        this.connectorName = connectorname;
    }
}
