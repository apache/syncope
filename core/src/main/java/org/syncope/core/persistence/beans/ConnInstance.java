/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.beans;

import com.thoughtworks.xstream.XStream;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
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
import org.hibernate.annotations.Type;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.util.ApplicationContextManager;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.ConnectorCapability;

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
     * ConnectorBundle-Name: Qualified name for the connector bundle.
     * Within a given deployment, the pair (ConnectorBundle-Name,
     * ConnectorBundle-Version) must be unique.
     */
    @Column(nullable = false)
    private String bundleName;

    /**
     * ConnectorBundle-Version: The version of the bundle. Within a given
     * deployment, the pair (ConnectorBundle-Name, ConnectorBundle-Version)
     * must be unique.
     */
    @Column(nullable = false)
    private String version;

    /**
     * The set of capabilities supported by this connector instance.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<ConnectorCapability> capabilities;

    /**
     * The main configuration for the connector instance.
     * This is directly implemented by the Configuration bean class which
     * contains annotated ConfigurationProperties (@ConfigurationProperty).
     */
    @Lob
    @Type(type = "org.hibernate.type.StringClobType")
    private String xmlConfiguration;

    private String displayName;

    @Lob
    private String serializedSyncToken;

    /**
     * Provisioning target resources associated to the connector.
     * The connector can be considered the resource's type.
     */
    @OneToMany(cascade = {CascadeType.REFRESH, CascadeType.MERGE},
    mappedBy = "connector")
    private List<TargetResource> resources;

    public ConnInstance() {
        super();

        capabilities = EnumSet.noneOf(ConnectorCapability.class);
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
        Set<ConnConfProperty> result = Collections.EMPTY_SET;

        try {
            ByteArrayInputStream tokenContentIS = new ByteArrayInputStream(
                    URLDecoder.decode(xmlConfiguration, "UTF-8").getBytes());

            XMLDecoder decoder = new XMLDecoder(tokenContentIS);
            Object object = decoder.readObject();
            decoder.close();

            result = (Set<ConnConfProperty>) object;
        } catch (Throwable t) {
            LOG.error("During connector properties deserialization", t);
        }

        return result;
    }

    public void setConfiguration(final Set<ConnConfProperty> configuration) {
        try {
            ByteArrayOutputStream tokenContentOS = new ByteArrayOutputStream();
            XMLEncoder encoder = new XMLEncoder(tokenContentOS);
            encoder.writeObject(configuration);
            encoder.flush();
            encoder.close();

            xmlConfiguration = URLEncoder.encode(tokenContentOS.toString(),
                    "UTF-8");
        } catch (Throwable t) {
            LOG.error("During connector properties serialization", t);
        }
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

    public List<TargetResource> getResources() {
        if (this.resources == null) {
            this.resources = new ArrayList<TargetResource>();
        }
        return this.resources;
    }

    public void setResources(List<TargetResource> resources) {
        this.resources = resources;
    }

    public boolean addResource(TargetResource resource) {
        if (this.resources == null) {
            this.resources = new ArrayList<TargetResource>();
        }
        return this.resources.add(resource);
    }

    public boolean removeResource(TargetResource resource) {
        if (this.resources == null) {
            return true;
        }
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

    public String getSerializedSyncToken() {
        return serializedSyncToken;
    }

    public void setSerializedSyncToken(final String serializedSyncToken) {
        this.serializedSyncToken = serializedSyncToken;
    }

    public SyncToken getSyncToken() {
        SyncToken result = null;

        if (serializedSyncToken != null) {
            ConfigurableApplicationContext context =
                    ApplicationContextManager.getApplicationContext();
            XStream xStream = context.getBean(XStream.class);
            try {
                result = (SyncToken) xStream.fromXML(
                        URLDecoder.decode(serializedSyncToken, "UTF-8"));
            } catch (Throwable t) {
                LOG.error("During attribute deserialization", t);
            }
        }

        return result;
    }

    public void setSyncToken(final SyncToken syncToken) {
        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();
        XStream xStream = context.getBean(XStream.class);
        try {
            serializedSyncToken = URLEncoder.encode(
                    xStream.toXML(syncToken), "UTF-8");
        } catch (Throwable t) {
            LOG.error("During attribute serialization", t);
        }
    }
}
