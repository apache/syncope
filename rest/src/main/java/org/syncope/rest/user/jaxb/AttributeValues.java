/*
 *  Copyright 2010 ilgrosso.
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
package org.syncope.rest.user.jaxb;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "values")
public class AttributeValues {

    @XmlElement(name = "value")
    private Set<String> elements;

    public AttributeValues() {
        elements = new HashSet<String>();
    }

    public AttributeValues(String value) {
        this();
        elements.add(value);
    }

    public AttributeValues(Collection<String> values) {
        this();
        elements.addAll(values);
    }

    public void addAttributeValue(String value) {
        elements.add(value);
    }

    public boolean removeAttributeValue(String value) {
        return elements.remove(value);
    }

    public void setElements(Set<String> elements) {
        this.elements = elements;
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}
