/*
 * Copyright 2011 John Yeary <jyeary@bluelotussoftware.com>.
 * Copyright 2011 Bluelotus Software, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.common.lib.jaxb;

import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class GenericMapEntryType<K, V> {

    private K key;

    private V value;

    public GenericMapEntryType() {
    }

    public GenericMapEntryType(final Map.Entry<K, V> e) {
        key = e.getKey();
        value = e.getValue();
    }

    @XmlElement
    public K getKey() {
        return key;
    }

    public void setKey(final K key) {
        this.key = key;
    }

    @XmlElement
    public V getValue() {
        return value;
    }

    public void setValue(final V value) {
        this.value = value;
    }
}
