/*
 *  Copyright 2010 fabio.
 * 
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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ConnectorInstance extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String minorVersion;

    public String getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(String majorVersion) {
        this.majorVersion = majorVersion;
    }

    public String getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(String minorVersion) {
        this.minorVersion = minorVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getXmlConfiguration() {
        return xmlConfiguration;
    }

    public void setXmlConfiguration(String xmlConfiguration) {
        this.xmlConfiguration = xmlConfiguration;
    }
    private String majorVersion;
    private String xmlConfiguration;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConnectorInstance other = (ConnectorInstance) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        if ((this.name == null)
                ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.minorVersion == null)
                ? (other.minorVersion != null)
                : !this.minorVersion.equals(other.minorVersion)) {
            return false;
        }
        if ((this.majorVersion == null)
                ? (other.majorVersion != null)
                : !this.majorVersion.equals(other.majorVersion)) {
            return false;
        }
        if ((this.xmlConfiguration == null)
                ? (other.xmlConfiguration != null)
                : !this.xmlConfiguration.equals(other.xmlConfiguration)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 83 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 83 * hash + (this.minorVersion != null
                ? this.minorVersion.hashCode() : 0);
        hash = 83 * hash + (this.majorVersion != null
                ? this.majorVersion.hashCode() : 0);
        hash = 83 * hash + (this.xmlConfiguration != null
                ? this.xmlConfiguration.hashCode() : 0);
        return hash;
    }
}
