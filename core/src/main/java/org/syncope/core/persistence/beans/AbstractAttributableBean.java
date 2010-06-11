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
package org.syncope.core.persistence.beans;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

@MappedSuperclass
public abstract class AbstractAttributableBean extends AbstractBaseBean {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
    orphanRemoval = true)
    protected Set<Attribute> attributes;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
    orphanRemoval = true)
    protected Set<DerivedAttribute> derivedAttributes;

    protected AbstractAttributableBean() {
        attributes = new HashSet<Attribute>();
        derivedAttributes = new HashSet<DerivedAttribute>();
    }

    public Attribute getAttribute(String schemaName)
            throws NoSuchElementException {

        Attribute result = null;
        Attribute attribute = null;
        for (Iterator<Attribute> itor = attributes.iterator();
                result == null && itor.hasNext();) {

            attribute = itor.next();

            if (schemaName.equals(attribute.getSchema().getName())) {
                result = attribute;
            }
        }

        return result;
    }

    public boolean addAttribute(Attribute attribute) {
        return attributes.add(attribute);
    }

    public boolean removeAttribute(Attribute attribute) {
        return attributes.remove(attribute);
    }

    public Set<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    public Set<DerivedAttribute> getDerivedAttributes() {
        return derivedAttributes;
    }

    public void setDerivedAttributes(Set<DerivedAttribute> derivedAttributes) {
        this.derivedAttributes = derivedAttributes;
    }
}
