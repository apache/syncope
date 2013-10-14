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
package org.apache.syncope.common.to;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import org.codehaus.jackson.annotate.JsonIgnore;

@XmlType
public abstract class AbstractAttributableTO extends ConnObjectTO {

    private static final long serialVersionUID = 4083884098736820255L;

    private long id;

    private List<AttributeTO> derivedAttributes;

    private List<AttributeTO> virtualAttributes;

    private Set<String> resources;

    private final List<PropagationStatusTO> propagationStatusTOs;

    protected AbstractAttributableTO() {
        super();

        derivedAttributes = new ArrayList<AttributeTO>();
        virtualAttributes = new ArrayList<AttributeTO>();
        resources = new HashSet<String>();
        propagationStatusTOs = new ArrayList<PropagationStatusTO>();
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @JsonIgnore
    public Map<String, AttributeTO> getDerivedAttributeMap() {
        Map<String, AttributeTO> result;

        if (derivedAttributes == null) {
            result = Collections.emptyMap();
        } else {
            result = new HashMap<String, AttributeTO>(derivedAttributes.size());
            for (AttributeTO attributeTO : derivedAttributes) {
                result.put(attributeTO.getSchema(), attributeTO);
            }
            result = Collections.unmodifiableMap(result);
        }

        return result;
    }

    @JsonIgnore
    public Map<String, AttributeTO> getVirtualAttributeMap() {
        Map<String, AttributeTO> result;

        if (derivedAttributes == null) {
            result = Collections.emptyMap();
        } else {
            result = new HashMap<String, AttributeTO>(virtualAttributes.size());
            for (AttributeTO attributeTO : virtualAttributes) {
                result.put(attributeTO.getSchema(), attributeTO);
            }
            result = Collections.unmodifiableMap(result);
        }

        return result;
    }

    public boolean addDerivedAttribute(final AttributeTO derivedAttribute) {
        return derivedAttributes.add(derivedAttribute);
    }

    public boolean removeDerivedAttribute(final AttributeTO derivedAttribute) {
        return derivedAttributes.remove(derivedAttribute);
    }

    @XmlElementWrapper(name = "derivedAttributes")
    @XmlElement(name = "attribute")
    public List<AttributeTO> getDerivedAttributes() {
        return derivedAttributes;
    }

    public void setDerivedAttributes(final List<AttributeTO> derivedAttributes) {
        this.derivedAttributes.clear();
        if (derivedAttributes != null && !derivedAttributes.isEmpty()) {
            this.derivedAttributes.addAll(derivedAttributes);
        }
    }

    public boolean addVirtualAttribute(final AttributeTO virtualAttribute) {
        return virtualAttributes.add(virtualAttribute);
    }

    public boolean removeVirtualAttribute(final AttributeTO virtualAttribute) {
        return virtualAttributes.remove(virtualAttribute);
    }

    @XmlElementWrapper(name = "virtualAttributes")
    @XmlElement(name = "attribute")
    public List<AttributeTO> getVirtualAttributes() {
        return virtualAttributes;
    }

    public void setVirtualAttributes(final List<AttributeTO> virtualAttributes) {
        this.virtualAttributes.clear();
        if (virtualAttributes != null && !virtualAttributes.isEmpty()) {
            this.virtualAttributes.addAll(virtualAttributes);
        }
    }

    public boolean addResource(final String resource) {
        return resources.add(resource);
    }

    public boolean removeResource(final String resource) {
        return resources.remove(resource);
    }

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    public Set<String> getResources() {
        return resources;
    }

    public void setResources(final Set<String> resources) {
        this.resources = resources;
    }

    public boolean addPropagationTO(final PropagationStatusTO status) {
        return propagationStatusTOs.add(status);
    }

    public boolean removePropagationTO(final String resource) {
        if (resource != null && getPropagationStatusTOs().isEmpty()) {
            final List<PropagationStatusTO> toBeRemoved = new ArrayList<PropagationStatusTO>();

            for (PropagationStatusTO propagationTO : getPropagationStatusTOs()) {
                if (resource.equals(propagationTO.getResource())) {
                    toBeRemoved.add(propagationTO);
                }
            }

            return propagationStatusTOs.removeAll(toBeRemoved);
        }
        return false;
    }

    @XmlElementWrapper(name = "propagationStatuses")
    @XmlElement(name = "propagationStatus")
    public List<PropagationStatusTO> getPropagationStatusTOs() {
        return propagationStatusTOs;
    }

    public void setPropagationStatusTOs(final List<PropagationStatusTO> propagationStatusTOs) {
        if (this.propagationStatusTOs != propagationStatusTOs) {
            this.propagationStatusTOs.clear();
            this.propagationStatusTOs.addAll(propagationStatusTOs);
        }
    }
}
