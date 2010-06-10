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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

@Entity
public class SyncopeRole implements Serializable {

    @EmbeddedId
    private SyncopeRolePK syncopeUserPK;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Attribute> attributes;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<DerivedAttribute> derivedAttributes;

    public SyncopeRole() {
        attributes = new HashSet<Attribute>();
        derivedAttributes = new HashSet<DerivedAttribute>();
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

    public SyncopeRolePK getSyncopeRolePK() {
        return syncopeUserPK;
    }

    public void setSyncopeRolePK(SyncopeRolePK syncopeRolePK) {
        this.syncopeUserPK = syncopeRolePK;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SyncopeRole other = (SyncopeRole) obj;
        if (this.syncopeUserPK != other.syncopeUserPK
                && (this.syncopeUserPK == null
                || !this.syncopeUserPK.equals(other.syncopeUserPK))) {

            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (this.syncopeUserPK != null
                ? this.syncopeUserPK.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "("
                + "syncopeUserPK=" + syncopeUserPK + ","
                + "attributes=" + attributes + ","
                + "derivedAttributes=" + derivedAttributes
                + ")";
    }
}
