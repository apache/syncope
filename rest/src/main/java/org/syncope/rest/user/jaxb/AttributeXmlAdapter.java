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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

class AttributeXmlAdapter extends XmlAdapter<Temp, Map<String, AttributeValues>> {

    @Override
    public Map<String, AttributeValues> unmarshal(Temp v) throws Exception {
        Map<String, AttributeValues> result = new HashMap<String, AttributeValues>();

        for (Item item : v.getEntry()) {
            result.put(item.getKey(), item.getValue());
        }

        return result;
    }

    @Override
    public Temp marshal(Map<String, AttributeValues> v) throws Exception {
        Temp result = new Temp();

        for (String key : v.keySet()) {
            result.addItem(new Item(key, v.get(key)));
        }

        return result;
    }
}

class Temp {

    @XmlElement(name = "attribute")
    private List<Item> entry = new ArrayList<Item>();

    public List<Item> getEntry() {
        return entry;
    }

    public void addItem(Item item) {
        entry.add(item);
    }
}

class Item {

    @XmlAttribute(name = "name")
    private String key;
    @XmlElement
    private AttributeValues values;

    public Item() {
        key = new String();
        values = new AttributeValues();
    }

    public Item(String key, AttributeValues value) {
        this.key = key;
        this.values = value;
    }

    public String getKey() {
        return key;
    }

    public AttributeValues getValue() {
        return values;
    }
}
