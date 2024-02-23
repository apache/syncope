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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnPoolConf;
import org.apache.syncope.common.lib.types.ConnectorCapability;

public class ConnInstanceTO implements EntityTO {

    private static final long serialVersionUID = 2707778645445168671L;

    private String key;

    private boolean errored;

    private String adminRealm;

    private String location;

    private String connectorName;

    private String bundleName;

    private String version;

    private final List<ConnConfProperty> conf = new ArrayList<>();

    private final Set<ConnectorCapability> capabilities = EnumSet.noneOf(ConnectorCapability.class);

    private String displayName;

    private Integer connRequestTimeout;

    private ConnPoolConf poolConf;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public boolean isErrored() {
        return errored;
    }

    public void setErrored(final boolean errored) {
        this.errored = errored;
    }

    public String getAdminRealm() {
        return adminRealm;
    }

    public void setAdminRealm(final String adminRealm) {
        this.adminRealm = adminRealm;
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

    @JacksonXmlElementWrapper(localName = "conf")
    @JacksonXmlProperty(localName = "property")
    public List<ConnConfProperty> getConf() {
        return this.conf;
    }

    @JsonIgnore
    public Optional<ConnConfProperty> getConf(final String schemaName) {
        return conf.stream().filter(property -> property.getSchema().getName().equals(schemaName)).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "capabilities")
    @JacksonXmlProperty(localName = "capability")
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

    public ConnPoolConf getPoolConf() {
        return poolConf;
    }

    public void setPoolConf(final ConnPoolConf poolConf) {
        this.poolConf = poolConf;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConnInstanceTO other = (ConnInstanceTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(errored, other.errored).
                append(adminRealm, other.adminRealm).
                append(location, other.location).
                append(connectorName, other.connectorName).
                append(bundleName, other.bundleName).
                append(version, other.version).
                append(conf, other.conf).
                append(capabilities, other.capabilities).
                append(displayName, other.displayName).
                append(connRequestTimeout, other.connRequestTimeout).
                append(poolConf, other.poolConf).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(errored).
                append(adminRealm).
                append(location).
                append(connectorName).
                append(bundleName).
                append(version).
                append(conf).
                append(capabilities).
                append(displayName).
                append(connRequestTimeout).
                append(poolConf).
                build();
    }
}
