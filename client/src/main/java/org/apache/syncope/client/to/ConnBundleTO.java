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
package org.apache.syncope.client.to;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.client.AbstractBaseBean;
import org.apache.syncope.types.ConnConfPropSchema;

@XmlRootElement(name = "connectorBundle")
@XmlType
public class ConnBundleTO extends AbstractBaseBean {

    private static final long serialVersionUID = 7215115961910138005L;

    private String displayName;

    private String bundleName;

    private String version;

    private String connectorName;

    private List<ConnConfPropSchema> properties = new ArrayList<ConnConfPropSchema>();

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(final String bundleName) {
        this.bundleName = bundleName;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(final String connectorName) {
        this.connectorName = connectorName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @XmlElementWrapper(name = "properties")
    @XmlElement(name = "connConfPropSchema")
    public List<ConnConfPropSchema> getProperties() {
        return properties;
    }

    public void setProperties(final List<ConnConfPropSchema> properties) {
        this.properties = properties;
    }

    public boolean addProperty(final ConnConfPropSchema property) {
        return properties.add(property);
    }

    public boolean removeProperty(final ConnConfPropSchema property) {
        return properties.remove(property);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }
}
