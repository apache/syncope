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
package org.syncope.core.beans;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class SyncopeUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER)
    private Set<UserAttribute> attributes;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER, mappedBy = "owner")
    private Set<UserDerivedAttribute> derivedAttributes;

    public SyncopeUser() {
        attributes = new HashSet<UserAttribute>();
        derivedAttributes = new HashSet<UserDerivedAttribute>();
    }

    public Long getId() {
        return id;
    }

    public UserAttribute getAttribute(String name)
            throws NoSuchElementException {

        UserAttribute result = null;
        UserAttribute userAttribute = null;
        for (Iterator<UserAttribute> itor = attributes.iterator();
                result == null && itor.hasNext();) {

            userAttribute = itor.next();

            if (name.equals(userAttribute.getSchema().getName())) {
                result = userAttribute;
            }
        }

        return result;
    }

    public Set<UserAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<UserAttribute> attributes) {
        this.attributes = attributes;
    }

    public Set<UserDerivedAttribute> getDerivedAttributes() {
        return derivedAttributes;
    }

    public void setDerivedAttributes(
            Set<UserDerivedAttribute> derivedAttributes) {

        this.derivedAttributes = derivedAttributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final SyncopeUser other = (SyncopeUser) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {

            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.id != null ? this.id.hashCode() : 0);

        return hash;
    }

    @Override
    public String toString() {
        return "("
                + "id=" + id + ","
                + "attributes=" + attributes
                + "derivedAttributes=" + derivedAttributes
                + ")";
    }
}
