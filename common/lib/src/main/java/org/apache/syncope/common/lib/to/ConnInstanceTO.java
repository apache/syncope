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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;

@XmlRootElement(name = "connInstance")
@XmlType
public class ConnInstanceTO extends AbstractBaseBean implements EntityTO {

    private static final long serialVersionUID = 2707778645445168671L;

    private String key;

    private String location;

    private String connectorName;

    private String bundleName;

    private String version;

    private final Set<ConnConfProperty> conf = new HashSet<>();

    private final Set<ConnectorCapability> capabilities = EnumSet.noneOf(ConnectorCapability.class);

    private String displayName;

    private Integer connRequestTimeout;

    private ConnPoolConfTO poolConf;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
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

    @XmlElementWrapper(name = "conf")
    @XmlElement(name = "property")
    @JsonProperty("conf")
    public Set<ConnConfProperty> getConf() {
        return this.conf;
    }

    @JsonIgnore
    public Map<String, ConnConfProperty> getConfMap() {
        Map<String, ConnConfProperty> result = new HashMap<>();

        for (ConnConfProperty prop : getConf()) {
            result.put(prop.getSchema().getName(), prop);
        }
        result = Collections.unmodifiableMap(result);

        return Collections.unmodifiableMap(result);
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
     * Get connector request timeout.
     * It is not applied in case of sync, full reconciliation and search.
     *
     * @return timeout.
     */
    public Integer getConnRequestTimeout() {
        return connRequestTimeout;
    }

    /**
     * Set connector request timeout.
     * It is not applied in case of sync, full reconciliation and search.
     *
     * @param connRequestTimeout timeout
     */
    public void setConnRequestTimeout(final Integer connRequestTimeout) {
        this.connRequestTimeout = connRequestTimeout;
    }

    public ConnPoolConfTO getPoolConf() {
        return poolConf;
    }

    public void setPoolConf(final ConnPoolConfTO poolConf) {
        this.poolConf = poolConf;
    }

}
