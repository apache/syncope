/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.to;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.syncope.AbstractBaseBean;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.ConnectorCapability;
import org.codehaus.jackson.annotate.JsonIgnore;

public class ConnInstanceTO extends AbstractBaseBean {

    private static final long serialVersionUID = 2707778645445168671L;

    private long id;

    private String bundleName;

    private String version;

    private String connectorName;

    private final Set<ConnConfProperty> configuration;

    private final Set<ConnectorCapability> capabilities;

    private String displayName;

    public ConnInstanceTO() {
        super();

        configuration = new HashSet<ConnConfProperty>();
        capabilities = EnumSet.noneOf(ConnectorCapability.class);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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
        Map<String, ConnConfProperty> result;

        if (getConfiguration() == null) {
            result = Collections.emptyMap();
        } else {
            result = new HashMap<String, ConnConfProperty>();
            for (ConnConfProperty prop : getConfiguration()) {
                result.put(prop.getSchema().getName(), prop);
            }
            result = Collections.unmodifiableMap(result);
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
        this.configuration.clear();
        if (configuration != null && !configuration.isEmpty()) {
            this.configuration.addAll(configuration);
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
