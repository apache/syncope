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
package org.apache.syncope.core.persistence.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.ConnectorCapability;
import org.apache.syncope.core.persistence.validation.entity.ConnInstanceCheck;
import org.apache.syncope.core.util.XMLSerializer;

@Entity
@ConnInstanceCheck
public class ConnInstance extends AbstractBaseBean {

    private static final long serialVersionUID = -2294708794497208872L;

    private static final int DEFAULT_TIMEOUT = 10;

    @Id
    private Long id;

    /**
     * URI identifying the local / remote ConnId location where the related connector bundle is found.
     */
    @Column(nullable = false)
    private String location;

    /**
     * Connector bundle class name.
     * Within a given location, the triple
     * (ConnectorBundle-Name, ConnectorBundle-Version, ConnectorBundle-Version) must be unique.
     */
    @Column(nullable = false)
    private String connectorName;

    /**
     * Qualified name for the connector bundle.
     * Within a given location, the triple
     * (ConnectorBundle-Name, ConnectorBundle-Version, ConnectorBundle-Version) must be unique.
     */
    @Column(nullable = false)
    private String bundleName;

    /**
     * Version of the bundle.
     * Within a given location, the triple
     * (ConnectorBundle-Name, ConnectorBundle-Version, ConnectorBundle-Version) must be unique.
     */
    @Column(nullable = false)
    private String version;

    /**
     * The set of capabilities supported by this connector instance.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Column(name = "capabilities")
    private Set<ConnectorCapability> capabilities;

    /**
     * The main configuration for the connector instance. This is directly implemented by the Configuration bean class
     * which contains annotated ConfigurationProperties.
     *
     * @see org.identityconnectors.framework.api.ConfigurationProperty
     */
    @Lob
    private String xmlConfiguration;

    @Column(unique = true)
    private String displayName;

    /**
     * External resources associated to the connector.
     */
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "connector")
    private List<ExternalResource> resources;

    /**
     * Connector request timeout. It is not applied in case of sync, full reconciliation and search.
     * DEFAULT_TIMEOUT is the default value to be used in case of unspecified timeout.
     */
    @Column
    private Integer connRequestTimeout = DEFAULT_TIMEOUT;

    public ConnInstance() {
        super();

        capabilities = new HashSet<ConnectorCapability>();
        resources = new ArrayList<ExternalResource>();
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

    public void setConnectorName(final String connectorName) {
        this.connectorName = connectorName;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(final String bundleName) {
        this.bundleName = bundleName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public Set<ConnConfProperty> getConfiguration() {
        Set<ConnConfProperty> result = XMLSerializer.<HashSet<ConnConfProperty>>deserialize(xmlConfiguration);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    public void setConfiguration(final Set<ConnConfProperty> configuration) {
        xmlConfiguration = XMLSerializer.serialize(new HashSet<ConnConfProperty>(configuration));
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public List<ExternalResource> getResources() {
        return this.resources;
    }

    public void setResources(final List<ExternalResource> resources) {
        this.resources.clear();
        if (resources != null && !resources.isEmpty()) {
            this.resources.addAll(resources);
        }
    }

    public boolean addResource(final ExternalResource resource) {
        return !this.resources.contains(resource) && this.resources.add(resource);
    }

    public boolean removeResource(final ExternalResource resource) {
        return this.resources.remove(resource);
    }

    public boolean addCapability(final ConnectorCapability capabitily) {
        return capabilities.add(capabitily);
    }

    public boolean removeCapability(final ConnectorCapability capabitily) {
        return capabilities.remove(capabitily);
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

    public Integer getConnRequestTimeout() {
        // DEFAULT_TIMEOUT will be returned in case of null timeout:
        // * instances created by the content loader 
        // * or with a timeout nullified explicitely
        return connRequestTimeout == null ? DEFAULT_TIMEOUT : connRequestTimeout;
    }

    public void setConnRequestTimeout(final Integer connRequestTimeout) {
        this.connRequestTimeout = connRequestTimeout;
    }
}
