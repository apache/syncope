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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.syncope.client.AbstractBaseBean;
import org.syncope.types.ConnectorCapability;

public class ConnectorInstanceTO extends AbstractBaseBean {

    private Long id;
    private String bundleName;
    private String version;
    private String connectorName;
    private Set<PropertyTO> configuration;
    private Set<ConnectorCapability> capabilities;

    public ConnectorInstanceTO() {
        configuration = new HashSet<PropertyTO>();
        capabilities = EnumSet.noneOf(ConnectorCapability.class);
    }

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
        return this.configuration;
    }

    public boolean addConfiguration(PropertyTO property) {
        return this.configuration.add(property);
    }

    public boolean removeConfiguration(PropertyTO property) {
        return this.configuration.remove(property);
    }

    public void setConfiguration(Set<PropertyTO> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            this.configuration.clear();
        } else {
            this.configuration = configuration;
        }
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorname) {
        this.connectorName = connectorname;
    }

    public boolean addCapability(ConnectorCapability capability) {
        return capabilities.add(capability);
    }

    public boolean removeCapability(ConnectorCapability capability) {
        return capabilities.remove(capability);
    }

    public Set<ConnectorCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<ConnectorCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            this.capabilities.clear();
        } else {
            this.capabilities = capabilities;
        }
    }
}
