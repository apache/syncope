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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.syncope.client.AbstractBaseBean;

public abstract class AbstractAttributableTO extends AbstractBaseBean {

    private static final long serialVersionUID = 4083884098736820255L;

    private long id;

    private List<AttributeTO> attributes;

    private List<AttributeTO> derivedAttributes;

    private List<AttributeTO> virtualAttributes;

    private Set<String> resources;

    protected AbstractAttributableTO() {
        super();

        attributes = new ArrayList<AttributeTO>();
        derivedAttributes = new ArrayList<AttributeTO>();
        virtualAttributes = new ArrayList<AttributeTO>();
        resources = new HashSet<String>();
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public boolean addAttribute(final AttributeTO attribute) {
        return attributes.add(attribute);
    }

    public boolean removeAttribute(final AttributeTO attribute) {
        return attributes.remove(attribute);
    }

    public List<AttributeTO> getAttributes() {
        return attributes;
    }

    public void setAttributes(final List<AttributeTO> attributes) {
        this.attributes = attributes;
    }

    @JsonIgnore
    public Map<String, List<String>> getAttributeMap() {
        Map<String, List<String>> result =
                new HashMap<String, List<String>>(attributes.size());
        for (AttributeTO attributeTO : attributes) {
            result.put(attributeTO.getSchema(), attributeTO.getValues());
        }

        return result;
    }

    @JsonIgnore
    public Map<String, List<String>> getDerivedAttributeMap() {
        Map<String, List<String>> result =
                new HashMap<String, List<String>>(derivedAttributes.size());
        for (AttributeTO attributeTO : derivedAttributes) {
            result.put(attributeTO.getSchema(), attributeTO.getValues());
        }

        return result;
    }

    @JsonIgnore
    public Map<String, List<String>> getVirtualAttributeMap() {
        Map<String, List<String>> result =
                new HashMap<String, List<String>>(virtualAttributes.size());
        for (AttributeTO attributeTO : virtualAttributes) {
            result.put(attributeTO.getSchema(), attributeTO.getValues());
        }

        return result;
    }

    public boolean addDerivedAttribute(final AttributeTO derivedAttribute) {
        return derivedAttributes.add(derivedAttribute);
    }

    public boolean removeDerivedAttribute(final AttributeTO derivedAttribute) {
        return derivedAttributes.remove(derivedAttribute);
    }

    public List<AttributeTO> getDerivedAttributes() {
        return derivedAttributes;
    }

    public void setDerivedAttributes(
            final List<AttributeTO> derivedAttributes) {

        this.derivedAttributes = derivedAttributes;
    }

    public boolean addVirtualAttribute(final AttributeTO virtualAttribute) {
        return virtualAttributes.add(virtualAttribute);
    }

    public boolean removeVirtualAttribute(final AttributeTO virtualAttribute) {
        return virtualAttributes.remove(virtualAttribute);
    }

    public List<AttributeTO> getVirtualAttributes() {
        return virtualAttributes;
    }

    public void setVirtualAttributes(
            final List<AttributeTO> virtualAttributes) {

        this.virtualAttributes = virtualAttributes;
    }

    public boolean addResource(final String resource) {
        return resources.add(resource);
    }

    public boolean removeResource(final String resource) {
        return resources.remove(resource);
    }

    public Set<String> getResources() {
        return resources;
    }

    public void setResources(final Set<String> resources) {
        this.resources = resources;
    }
}
