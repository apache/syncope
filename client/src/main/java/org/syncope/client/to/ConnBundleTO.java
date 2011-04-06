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
package org.syncope.client.to;

import org.syncope.types.ConnConfPropSchema;
import java.util.ArrayList;
import java.util.List;
import org.syncope.client.AbstractBaseBean;

public class ConnBundleTO extends AbstractBaseBean {

    private String displayName;

    private String bundleName;

    private String version;

    private String connectorName;

    private List<ConnConfPropSchema> properties;

    public ConnBundleTO() {
        properties = new ArrayList<ConnConfPropSchema>();
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<ConnConfPropSchema> getProperties() {
        return properties;
    }

    public void setProperties(List<ConnConfPropSchema> properties) {
        this.properties = properties;
    }

    public boolean addProperty(ConnConfPropSchema property) {
        return properties.add(property);
    }

    public boolean removeProperty(ConnConfPropSchema property) {
        return properties.remove(property);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
