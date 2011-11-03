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

import org.syncope.types.ConnConfProperty;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.syncope.client.AbstractBaseBean;
import org.syncope.types.ConnectorCapability;

public class ConnInstanceTO extends AbstractBaseBean {

    private static final long serialVersionUID = 2707778645445168671L;

    private Long id;

    private String bundleName;

    private String version;

    private String connectorName;

    private Set<ConnConfProperty> configuration;

    private Set<ConnectorCapability> capabilities;

    private String displayName;

    public ConnInstanceTO() {
        super();

        configuration = new HashSet<ConnConfProperty>();
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

    public Set<ConnConfProperty> getConfiguration() {
        return this.configuration;
    }

    @JsonIgnore
    public Map<String, ConnConfProperty> getConfigurationMap() {
        Map<String, ConnConfProperty> result =
                new HashMap<String, ConnConfProperty>();
        for (ConnConfProperty prop : getConfiguration()) {
            result.put(prop.getSchema().getName(), prop);
        }

        return result;
    }

    public boolean addConfiguration(ConnConfProperty property) {
        return this.configuration.add(property);
    }

    public boolean removeConfiguration(ConnConfProperty property) {
        return this.configuration.remove(property);
    }

    public void setConfiguration(Set<ConnConfProperty> configuration) {
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

    public void setCapabilities(final Set<ConnectorCapability> capabilities) {
        this.capabilities.clear();
        if (capabilities != null && !capabilities.isEmpty()) {
            this.capabilities.addAll(capabilities);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
