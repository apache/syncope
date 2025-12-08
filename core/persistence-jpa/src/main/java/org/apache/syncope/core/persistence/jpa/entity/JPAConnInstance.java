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
package org.apache.syncope.core.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnPoolConf;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.common.validation.ConnInstanceCheck;
import org.apache.syncope.core.persistence.jpa.converters.ConnConfPropertyListConverter;
import org.apache.syncope.core.persistence.jpa.converters.ConnPoolConfConverter;
import org.apache.syncope.core.persistence.jpa.converters.ConnectorCapabilitySetConverter;

@Entity
@Table(name = JPAConnInstance.TABLE)
@ConnInstanceCheck
public class JPAConnInstance extends AbstractGeneratedKeyEntity implements ConnInstance {

    private static final long serialVersionUID = -2294708794497208872L;

    public static final String TABLE = "ConnInstance";

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm adminRealm;

    /**
     * URI identifying the local / remote ConnId location where the related connector bundle is found.
     */
    @NotNull
    private String location;

    /**
     * Connector class name.
     */
    @NotNull
    private String connectorName;

    /**
     * Connector bundle name.
     */
    @NotNull
    private String bundleName;

    /**
     * Connector bundle version.
     */
    @NotNull
    private String version;

    @Convert(converter = ConnectorCapabilitySetConverter.class)
    @Lob
    private Set<ConnectorCapability> capabilities = new HashSet<>();

    /**
     * The main configuration for the connector instance. This is directly implemented by the Configuration bean class
     * which contains annotated ConfigurationProperties.
     *
     * @see org.identityconnectors.framework.api.ConfigurationProperty
     */
    @Convert(converter = ConnConfPropertyListConverter.class)
    @Lob
    private List<ConnConfProperty> jsonConf = new ArrayList<>();

    @Column(unique = true)
    private String displayName;

    /**
     * Connection request timeout.
     * It is not applied in case of sync, full reconciliation and search.
     * DEFAULT_TIMEOUT if null.
     */
    private Integer connRequestTimeout = DEFAULT_TIMEOUT;

    @Convert(converter = ConnPoolConfConverter.class)
    @Lob
    private ConnPoolConf poolConf;

    @Override
    public Realm getAdminRealm() {
        return adminRealm;
    }

    @Override
    public void setAdminRealm(final Realm adminRealm) {
        checkType(adminRealm, JPARealm.class);
        this.adminRealm = (JPARealm) adminRealm;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public void setLocation(final String location) {
        this.location = location;
    }

    @Override
    public String getConnectorName() {
        return connectorName;
    }

    @Override
    public void setConnectorName(final String connectorName) {
        this.connectorName = connectorName;
    }

    @Override
    public String getBundleName() {
        return bundleName;
    }

    @Override
    public void setBundleName(final String bundleName) {
        this.bundleName = bundleName;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public List<ConnConfProperty> getConf() {
        return jsonConf;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public Set<ConnectorCapability> getCapabilities() {
        return capabilities;
    }

    @Override
    public Integer getConnRequestTimeout() {
        return Optional.ofNullable(connRequestTimeout).orElse(DEFAULT_TIMEOUT);
    }

    @Override
    public void setConnRequestTimeout(final Integer timeout) {
        this.connRequestTimeout = timeout;
    }

    @Override
    public ConnPoolConf getPoolConf() {
        return poolConf;
    }

    @Override
    public void setPoolConf(final ConnPoolConf poolConf) {
        this.poolConf = poolConf;
    }
}
