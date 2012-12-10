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
package org.apache.syncope.mod;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.AbstractBaseBean;

/**
 * Abstract base class for objects that can have attributes removed, added or updated.
 *
 * Attributes can be regular attributes, derived attributes, virtual attributes and resources.
 */
@XmlType
public abstract class AbstractAttributableMod extends AbstractBaseBean {

    private static final long serialVersionUID = 3241118574016303198L;
    protected long id;
    protected Set<AttributeMod> attributesToBeUpdated = new HashSet<AttributeMod>();
    protected Set<String> attributesToBeRemoved = new HashSet<String>();
    protected Set<String> derivedAttributesToBeAdded = new HashSet<String>();
    protected Set<String> derivedAttributesToBeRemoved = new HashSet<String>();
    protected Set<AttributeMod> virtualAttributesToBeUpdated = new HashSet<AttributeMod>();
    protected Set<String> virtualAttributesToBeRemoved = new HashSet<String>();
    protected Set<String> resourcesToBeAdded = new HashSet<String>();
    protected Set<String> resourcesToBeRemoved = new HashSet<String>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Convenience method for removing entire attribute instead removing each value in an AttributeMod object
     *
     * @param attribute (schema) of attribute to be removed.
     * @return true on success.
     */
    public boolean addAttributeToBeRemoved(String attribute) {
        return attributesToBeRemoved.add(attribute);
    }

    /**
     * Convenience method for removing entire attribute instead removing each value in an AttributeMod object
     *
     * @param attribute (schema) of attribute to be removed.
     * @return true on success.
     */
    public boolean removeAttributeToBeRemoved(String attribute) {
        return attributesToBeRemoved.remove(attribute);
    }

    @XmlElement(name = "attribute")
    @XmlElementWrapper(name = "attributesToBeRemoved")
    public Set<String> getAttributesToBeRemoved() {
        return attributesToBeRemoved;
    }

    /**
     * Add an attribute modification object. AttributeMod itself indicates how the attribute should be modified.
     *
     * @param attribute modification object
     * @see AttributeMod
     * @return true on success
     */
    public boolean addAttributeToBeUpdated(AttributeMod attribute) {
        return attributesToBeUpdated.add(attribute);
    }

    /**
     * Remove an attribute modification object. AttributeMod itself indicates how the attribute should be modified.
     *
     * @param attribute modification object
     * @see AttributeMod
     * @return true on success
     */
    public boolean removeAttributeToBeUpdated(AttributeMod attribute) {
        return attributesToBeUpdated.remove(attribute);
    }

    @XmlElement(name = "attribute")
    @XmlElementWrapper(name = "attributesToBeUpdated")
    public Set<AttributeMod> getAttributesToBeUpdated() {
        return attributesToBeUpdated;
    }

    /**
     * Add an attribute modification object. AttributeMod itself indicates how the attribute should be modified.
     *
     * @param derivedAttribute modification object
     * @see AttributeMod
     * @return true on success
     */
    public boolean addDerivedAttributeToBeAdded(String derivedAttribute) {
        return derivedAttributesToBeAdded.add(derivedAttribute);
    }

    /**
     * Add a derivedattribute. Value is calculated by its definition.
     *
     * @param derivedAttribute
     * @return true on success
     */
    public boolean removeDerivedAttributeToBeAdded(String derivedAttribute) {
        return derivedAttributesToBeAdded.remove(derivedAttribute);
    }

    @XmlElement(name = "attribute")
    @XmlElementWrapper(name = "derivedAttributesToBeAdded")
    public Set<String> getDerivedAttributesToBeAdded() {
        return derivedAttributesToBeAdded;
    }

    public boolean addDerivedAttributeToBeRemoved(String derivedAttribute) {
        return derivedAttributesToBeRemoved.add(derivedAttribute);
    }

    public boolean removeDerivedAttributeToBeRemoved(String derivedAttribute) {
        return derivedAttributesToBeRemoved.remove(derivedAttribute);
    }

    @XmlElement(name = "attribute")
    @XmlElementWrapper(name = "derivedAttributesToBeRemoved")
    public Set<String> getDerivedAttributesToBeRemoved() {
        return derivedAttributesToBeRemoved;
    }

    @XmlElement(name = "attribute")
    @XmlElementWrapper(name = "virtualAttributesToBeRemoved")
    public Set<String> getVirtualAttributesToBeRemoved() {
        return virtualAttributesToBeRemoved;
    }

    public boolean addVirtualAttributeToBeRemoved(String virtualAttributeToBeRemoved) {

        return virtualAttributesToBeRemoved.add(virtualAttributeToBeRemoved);
    }

    public boolean removeVirtualAttributeToBeRemoved(String virtualAttributeToBeRemoved) {

        return virtualAttributesToBeRemoved.remove(virtualAttributeToBeRemoved);
    }

    public boolean addVirtualAttributeToBeUpdated(AttributeMod virtualAttributeToBeUpdated) {

        return virtualAttributesToBeUpdated.add(virtualAttributeToBeUpdated);
    }

    public boolean removeVirtualAttributeToBeUpdated(AttributeMod virtualAttributeToBeUpdated) {

        return virtualAttributesToBeUpdated.remove(virtualAttributeToBeUpdated);
    }

    @XmlElement(name = "attribute")
    @XmlElementWrapper(name = "virtualAttributesToBeUpdated")
    public Set<AttributeMod> getVirtualAttributesToBeUpdated() {
        return virtualAttributesToBeUpdated;
    }

    public boolean addResourceToBeAdded(String resource) {
        return resourcesToBeAdded.add(resource);
    }

    public boolean removeResourceToBeAdded(String resource) {
        return resourcesToBeAdded.remove(resource);
    }

    @XmlElement(name = "resource")
    @XmlElementWrapper(name = "resourcesToBeAdded")
    public Set<String> getResourcesToBeAdded() {
        return resourcesToBeAdded;
    }

    public boolean addResourceToBeRemoved(String resource) {
        return resourcesToBeRemoved.add(resource);
    }

    public boolean removeResourceToBeRemoved(String resource) {
        return resourcesToBeRemoved.remove(resource);
    }

    @XmlElement(name = "attribute")
    @XmlElementWrapper(name = "resourcesToBeRemoved")
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
