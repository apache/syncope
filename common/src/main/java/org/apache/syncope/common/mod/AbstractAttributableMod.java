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
package org.apache.syncope.common.mod;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;

/**
 * Abstract base class for objects that can have attributes removed, added or updated.
 *
 * Attributes can be regular attributes, derived attributes, virtual attributes and resources.
 */
@XmlType
public abstract class AbstractAttributableMod extends AbstractBaseBean {

    private static final long serialVersionUID = 3241118574016303198L;

    protected long id;

    protected Set<AttributeMod> attributesToBeUpdated;

    protected Set<String> attributesToBeRemoved;

    protected Set<String> derivedAttributesToBeAdded;

    protected Set<String> derivedAttributesToBeRemoved;

    protected Set<AttributeMod> virtualAttributesToBeUpdated;

    protected Set<String> virtualAttributesToBeRemoved;

    protected Set<String> resourcesToBeAdded;

    protected Set<String> resourcesToBeRemoved;

    /**
     * All attributes are initialized to empty sets.
     */
    public AbstractAttributableMod() {
        super();

        attributesToBeUpdated = new HashSet<AttributeMod>();
        attributesToBeRemoved = new HashSet<String>();
        derivedAttributesToBeAdded = new HashSet<String>();
        derivedAttributesToBeRemoved = new HashSet<String>();
        virtualAttributesToBeUpdated = new HashSet<AttributeMod>();
        virtualAttributesToBeRemoved = new HashSet<String>();
        resourcesToBeAdded = new HashSet<String>();
        resourcesToBeRemoved = new HashSet<String>();
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @XmlElementWrapper(name = "attributesToBeRemoved")
    @XmlElement(name = "attribute")
    @JsonProperty("attributesToBeRemoved")
    public Set<String> getAttributesToBeRemoved() {
        return attributesToBeRemoved;
    }

    @XmlElementWrapper(name = "attributesToBeUpdated")
    @XmlElement(name = "attributeMod")
    @JsonProperty("attributesToBeUpdated")
    public Set<AttributeMod> getAttributesToBeUpdated() {
        return attributesToBeUpdated;
    }

    @XmlElementWrapper(name = "derivedAttributesToBeAdded")
    @XmlElement(name = "attributeName")
    @JsonProperty("derivedAttributesToBeAdded")
    public Set<String> getDerivedAttributesToBeAdded() {
        return derivedAttributesToBeAdded;
    }

    @XmlElementWrapper(name = "derivedAttributesToBeRemoved")
    @XmlElement(name = "attribute")
    @JsonProperty("derivedAttributesToBeRemoved")
    public Set<String> getDerivedAttributesToBeRemoved() {
        return derivedAttributesToBeRemoved;
    }

    @XmlElementWrapper(name = "virtualAttributesToBeRemoved")
    @XmlElement(name = "attribute")
    @JsonProperty("virtualAttributesToBeRemoved")
    public Set<String> getVirtualAttributesToBeRemoved() {
        return virtualAttributesToBeRemoved;
    }

    @XmlElementWrapper(name = "virtualAttributesToBeUpdated")
    @XmlElement(name = "attribute")
    @JsonProperty("virtualAttributesToBeUpdated")
    public Set<AttributeMod> getVirtualAttributesToBeUpdated() {
        return virtualAttributesToBeUpdated;
    }

    @XmlElementWrapper(name = "resourcesToBeAdded")
    @XmlElement(name = "resource")
    @JsonProperty("resourcesToBeAdded")
    public Set<String> getResourcesToBeAdded() {
        return resourcesToBeAdded;
    }

    @XmlElementWrapper(name = "resourcesToBeRemoved")
    @XmlElement(name = "resource")
    @JsonProperty("resourcesToBeRemoved")
    public Set<String> getResourcesToBeRemoved() {
        return resourcesToBeRemoved;
    }

    /**
     * @return true is all backing Sets are empty.
     */
    public boolean isEmpty() {
        return attributesToBeUpdated.isEmpty() && attributesToBeRemoved.isEmpty()
                && derivedAttributesToBeAdded.isEmpty() && derivedAttributesToBeRemoved.isEmpty()
                && virtualAttributesToBeUpdated.isEmpty() && virtualAttributesToBeRemoved.isEmpty()
                && resourcesToBeAdded.isEmpty() && resourcesToBeRemoved.isEmpty();
    }
}
