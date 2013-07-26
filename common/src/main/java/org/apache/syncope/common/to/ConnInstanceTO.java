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
package org.apache.syncope.common.to;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.ConnectorCapability;

@XmlRootElement(name = "connInstance")
@XmlType
public class ConnInstanceTO extends AbstractBaseBean {

    private static final long serialVersionUID = 2707778645445168671L;

    private long id;

    private String location;

    private String connectorName;

    private String bundleName;

    private String version;

    private final Set<ConnConfProperty> configuration;

    private final Set<ConnectorCapability> capabilities;

    private String displayName;

    private Integer connRequestTimeout;

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

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(final String connectorname) {
        this.connectorName = connectorname;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(final String bundlename) {
        this.bundleName = bundlename;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    @XmlElementWrapper(name = "configuration")
    @XmlElement(name = "property")
    @JsonProperty("configuration")
    public Set<ConnConfProperty> getConfiguration() {
        return this.configuration;
    }

    @JsonIgnore
    public Map<String, ConnConfProperty> getConfigurationMap() {
        Map<String, ConnConfProperty> result;

        if (getConfiguration() == null) {
            result = Collections.<String, ConnConfProperty>emptyMap();
        } else {
            result = new HashMap<String, ConnConfProperty>();
            for (ConnConfProperty prop : getConfiguration()) {
                result.put(prop.getSchema().getName(), prop);
            }
            result = Collections.unmodifiableMap(result);
        }

        return result;
    }

    @XmlElementWrapper(name = "capabilities")
    @XmlElement(name = "capability")
    @JsonProperty("capabilities")
    public Set<ConnectorCapability> getCapabilities() {
        return capabilities;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get connector request timeout. It is not applied in case of sync, full reconciliation and search.
     *
     * @return timeout.
     */
    public Integer getConnRequestTimeout() {
        return connRequestTimeout;
    }

    /**
     * Set connector request timeout. It is not applied in case of sync, full reconciliation and search.
     *
     * @param timeout.
     */
    public void setConnRequestTimeout(final Integer connRequestTimeout) {
        this.connRequestTimeout = connRequestTimeout;
    }
}
