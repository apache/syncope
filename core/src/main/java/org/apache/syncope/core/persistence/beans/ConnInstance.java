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
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.ConnectorCapability;
import org.apache.syncope.util.XMLSerializer;

@Entity
public class ConnInstance extends AbstractBaseBean {

    private static final long serialVersionUID = -2294708794497208872L;

    @Id
    private Long id;

    /**
     * Connector class name prefix used to retrieve configuration bean.
     */
    @Column(nullable = false)
    private String connectorName;

    /**
     * ConnectorBundle-Name: Qualified name for the connector bundle. Within a given deployment, the pair
     * (ConnectorBundle-Name, ConnectorBundle-Version) must be unique.
     */
    @Column(nullable = false)
    private String bundleName;

    /**
     * ConnectorBundle-Version: The version of the bundle. Within a given deployment, the pair (ConnectorBundle-Name,
     * ConnectorBundle-Version) must be unique.
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
     * which contains annotated ConfigurationProperties (@ConfigurationProperty).
     */
    @Lob
    private String xmlConfiguration;

    @Column(unique = true)
    private String displayName;

    /**
     * External resources associated to the connector.
     */
    @OneToMany(cascade = { CascadeType.ALL }, mappedBy = "connector")
    private List<ExternalResource> resources;

    public ConnInstance() {
        super();
        capabilities = new HashSet<ConnectorCapability>();
        resources = new ArrayList<ExternalResource>();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String majorVersion) {
        this.version = majorVersion;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
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

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<ExternalResource> getResources() {
        return this.resources;
    }

    public void setResources(List<ExternalResource> resources) {
        this.resources.clear();
        if (resources != null && !resources.isEmpty()) {
            this.resources.addAll(resources);
        }
    }

    public boolean addResource(ExternalResource resource) {
        return !this.resources.contains(resource) && this.resources.add(resource);
    }

    public boolean removeResource(ExternalResource resource) {
        return this.resources.remove(resource);
    }

    public boolean addCapability(ConnectorCapability capabitily) {
        return capabilities.add(capabitily);
    }

    public boolean removeCapability(ConnectorCapability capabitily) {
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
}
