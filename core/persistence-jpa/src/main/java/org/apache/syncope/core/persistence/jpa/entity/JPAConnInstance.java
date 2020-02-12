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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ConnPoolConf;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.jpa.validation.entity.ConnInstanceCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;

@Entity
@Table(name = JPAConnInstance.TABLE)
@ConnInstanceCheck
public class JPAConnInstance extends AbstractGeneratedKeyEntity implements ConnInstance {

    private static final long serialVersionUID = -2294708794497208872L;

    public static final String TABLE = "ConnInstance";

    private static final int DEFAULT_TIMEOUT = 10;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm adminRealm;

    /**
     * URI identifying the local / remote ConnId location where the related connector bundle is found.
     */
    @NotNull
    private String location;

    /**
     * Connector bundle class name.
     * Within a given location, the triple
     * (ConnectorBundle-Name, ConnectorBundle-Version, ConnectorBundle-Version) must be unique.
     */
    @NotNull
    private String connectorName;

    /**
     * Qualified name for the connector bundle.
     * Within a given location, the triple
     * (ConnectorBundle-Name, ConnectorBundle-Version, ConnectorBundle-Version) must be unique.
     */
    @NotNull
    private String bundleName;

    /**
     * Version of the bundle.
     * Within a given location, the triple
     * (ConnectorBundle-Name, ConnectorBundle-Version, ConnectorBundle-Version) must be unique.
     */
    @NotNull
    private String version;

    /**
     * The set of capabilities supported by this connector instance.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Column(name = "capability")
    @CollectionTable(name = "ConnInstance_capabilities",
            joinColumns =
            @JoinColumn(name = "connInstance_id", referencedColumnName = "id"))
    private Set<ConnectorCapability> capabilities = new HashSet<>();

    /**
     * The main configuration for the connector instance. This is directly implemented by the Configuration bean class
     * which contains annotated ConfigurationProperties.
     *
     * @see org.identityconnectors.framework.api.ConfigurationProperty
     */
    @Lob
    private String jsonConf;

    @Column(unique = true)
    private String displayName;

    /**
     * External resources associated to the connector.
     */
    @OneToMany(cascade = { CascadeType.ALL }, mappedBy = "connector")
    private List<JPAExternalResource> resources = new ArrayList<>();

    /**
     * Connector request timeout. It is not applied in case of sync, full reconciliation and search.
     * DEFAULT_TIMEOUT is the default value to be used in case of unspecified timeout.
     */
    private Integer connRequestTimeout = DEFAULT_TIMEOUT;

    private JPAConnPoolConf poolConf;

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
    public Set<ConnConfProperty> getConf() {
        Set<ConnConfProperty> configuration = new HashSet<>();
        if (!StringUtils.isBlank(jsonConf)) {
            configuration.addAll(List.of(POJOHelper.deserialize(jsonConf, ConnConfProperty[].class)));
        }

        return configuration;
    }

    @Override
    public void setConf(final Collection<ConnConfProperty> conf) {
        jsonConf = POJOHelper.serialize(new HashSet<>(conf));
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
    public List<? extends ExternalResource> getResources() {
        return this.resources;
    }

    @Override
    public boolean add(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        return this.resources.contains((JPAExternalResource) resource)
                || this.resources.add((JPAExternalResource) resource);
    }

    @Override
    public Set<ConnectorCapability> getCapabilities() {
        return capabilities;
    }

    @Override
    public Integer getConnRequestTimeout() {
        // DEFAULT_TIMEOUT will be returned in case of null timeout:
        // * instances created by the content loader
        // * or with a timeout nullified explicitely
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
        checkType(poolConf, JPAConnPoolConf.class);
        this.poolConf = (JPAConnPoolConf) poolConf;
    }
}
