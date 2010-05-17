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
package org.syncope.rest.user.jaxb;

import java.util.HashMap;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "userAttributes")
public class Attributes {

    @XmlJavaTypeAdapter(AttributeXmlAdapter.class)
    @XmlElement(name = "attributes")
    private HashMap<String, AttributeValues> elements;

    public Attributes() {
        elements = new HashMap<String, AttributeValues>();
    }

    public void addUserAttribute(String name, String value) {
        elements.put(name, new AttributeValues(value));
    }

    public void addUserAttribute(String name, Set<String> values) {
        elements.put(name, new AttributeValues(values));
    }

    public void addUserAttribute(String name, AttributeValues values) {
        elements.put(name, values);
    }

    public AttributeValues getAttributeValues(String name) {
        return elements.get(name);
    }

    @Override
    public String toString() {
        return elements.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Attributes)) {
            return false;
        }

        Attributes other = (Attributes) obj;
        return elements.equals(other.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }
}
