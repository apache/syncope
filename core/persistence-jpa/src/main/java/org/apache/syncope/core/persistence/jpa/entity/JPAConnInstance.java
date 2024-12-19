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

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnPoolConf;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.common.validation.ConnInstanceCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAConnInstance.TABLE)
@ConnInstanceCheck
public class JPAConnInstance extends AbstractGeneratedKeyEntity implements ConnInstance {

    private static final long serialVersionUID = -2294708794497208872L;

    public static final String TABLE = "ConnInstance";

    protected static final TypeReference<Set<ConnectorCapability>> CONNECTOR_CAPABILITY_TYPEREF =
            new TypeReference<Set<ConnectorCapability>>() {
    };

    protected static final TypeReference<List<ConnConfProperty>> CONN_CONF_PROPS_TYPEREF =
            new TypeReference<List<ConnConfProperty>>() {
    };

    private static final int DEFAULT_TIMEOUT = 10;

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

    @Lob
    private String capabilities;

    private final Set<ConnectorCapability> capabilitiesSet = new HashSet<>();

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
     * Connection request timeout.
     * It is not applied in case of sync, full reconciliation and search.
     * DEFAULT_TIMEOUT if null.
     */
    private Integer connRequestTimeout = DEFAULT_TIMEOUT;

    private String poolConf;

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
        return StringUtils.isNotBlank(jsonConf)
                ? POJOHelper.deserialize(jsonConf, CONN_CONF_PROPS_TYPEREF)
                : new ArrayList<>();
    }

    @Override
    public void setConf(final List<ConnConfProperty> conf) {
        jsonConf = POJOHelper.serialize(conf);
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
        return resources;
    }

    @Override
    public boolean add(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        return resources.contains((JPAExternalResource) resource) || resources.add((JPAExternalResource) resource);
    }

    @Override
    public Set<ConnectorCapability> getCapabilities() {
        return capabilitiesSet;
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
        return Optional.ofNullable(poolConf).map(pc -> POJOHelper.deserialize(pc, ConnPoolConf.class)).orElse(null);
    }

    @Override
    public void setPoolConf(final ConnPoolConf poolConf) {
        this.poolConf = Optional.ofNullable(poolConf).map(POJOHelper::serialize).orElse(null);
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getCapabilities().clear();
        }
        if (capabilities != null) {
            getCapabilities().addAll(POJOHelper.deserialize(capabilities, CONNECTOR_CAPABILITY_TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2list(true);
    }

    @PrePersist
    @PreUpdate
    public void list2json() {
        capabilities = POJOHelper.serialize(getCapabilities());
    }
}
