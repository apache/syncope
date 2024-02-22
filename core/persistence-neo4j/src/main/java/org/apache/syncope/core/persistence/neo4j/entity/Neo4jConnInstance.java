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
package org.apache.syncope.core.persistence.neo4j.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jConnInstance.NODE)
@ConnInstanceCheck
public class Neo4jConnInstance extends AbstractGeneratedKeyNode implements ConnInstance {

    private static final long serialVersionUID = -2294708794497208872L;

    public static final String NODE = "ConnInstance";

    protected static final TypeReference<Set<ConnectorCapability>> TYPEREF =
            new TypeReference<Set<ConnectorCapability>>() {
    };

    private static final int DEFAULT_TIMEOUT = 10;

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

    private String capabilities;

    @Transient
    private Set<ConnectorCapability> capabilitiesSet = new HashSet<>();

    /**
     * The main configuration for the connector instance. This is directly implemented by the Configuration bean class
     * which contains annotated ConfigurationProperties.
     *
     * @see org.identityconnectors.framework.api.ConfigurationProperty
     */
    private String jsonConf;

    private String displayName;

    /**
     * Connector request timeout. It is not applied in case of sync, full reconciliation and search.
     * DEFAULT_TIMEOUT is the default value to be used in case of unspecified timeout.
     */
    private Integer connRequestTimeout = DEFAULT_TIMEOUT;

    private String poolConf;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jRealm adminRealm;

    /**
     * External resources associated to the connector.
     */
    @Relationship(type = Neo4jExternalResource.RESOURCE_CONNECTOR_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jExternalResource> resources = new ArrayList<>();

    @Override
    public Realm getAdminRealm() {
        return adminRealm;
    }

    @Override
    public void setAdminRealm(final Realm adminRealm) {
        checkType(adminRealm, Neo4jRealm.class);
        this.adminRealm = (Neo4jRealm) adminRealm;
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
        return resources;
    }

    @Override
    public boolean add(final ExternalResource resource) {
        checkType(resource, Neo4jExternalResource.class);
        return resources.contains((Neo4jExternalResource) resource) || resources.add((Neo4jExternalResource) resource);
    }

    @Override
    public Set<ConnectorCapability> getCapabilities() {
        return capabilitiesSet;
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
            getCapabilities().addAll(POJOHelper.deserialize(capabilities, TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    public void postSave() {
        json2list(true);
    }

    public void list2json() {
        capabilities = POJOHelper.serialize(getCapabilities());
    }
}
