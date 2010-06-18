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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
public class ConnectorInstance extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    /**
     * Connector class name prefix used to retrieve configuration bean
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
     * The main configuration for the connector instance.
     * This is directly implemented by the Configuration bean class which
     * contains annotated ConfigurationProperties (@ConfigurationProperty).
     */
    @Lob
    @Column(nullable = false)
    private String xmlConfiguration;

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

    public String getXmlConfiguration() {
        return xmlConfiguration;
    }

    public void setXmlConfiguration(String xmlConfiguration) {
        this.xmlConfiguration = xmlConfiguration;
    }

    public Long getId() {
        return id;
    }
}
