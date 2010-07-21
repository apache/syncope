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
package org.syncope.core.persistence.beans.membership;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {"syncopeUser_id", "syncopeRole_id"}))
public class Membership extends AbstractAttributable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private SyncopeUser syncopeUser;
    @ManyToOne
    private SyncopeRole syncopeRole;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER, mappedBy = "owner")
    private Set<MembershipAttribute> attributes;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER, mappedBy = "owner")
    private Set<MembershipDerivedAttribute> derivedAttributes;

    public Membership() {
        attributes = new HashSet<MembershipAttribute>();
        derivedAttributes = new HashSet<MembershipDerivedAttribute>();
    }

    public Long getId() {
        return id;
    }

    public SyncopeRole getSyncopeRole() {
        return syncopeRole;
    }

    public void setSyncopeRole(SyncopeRole syncopeRole) {
        this.syncopeRole = syncopeRole;
    }

    public SyncopeUser getSyncopeUser() {
        return syncopeUser;
    }

    public void setSyncopeUser(SyncopeUser syncopeUser) {
        this.syncopeUser = syncopeUser;
    }

    @Override
    public <T extends AbstractAttribute> boolean addAttribute(T attribute) {
        return attributes.add((MembershipAttribute) attribute);
    }

    @Override
    public <T extends AbstractAttribute> boolean removeAttribute(T attribute) {
        return attributes.remove((MembershipAttribute) attribute);
    }

    @Override
    public Set<? extends AbstractAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Set<? extends AbstractAttribute> attributes) {
        this.attributes = (Set<MembershipAttribute>) attributes;
    }

    @Override
    public <T extends AbstractDerivedAttribute> boolean addDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.add(
                (MembershipDerivedAttribute) derivedAttribute);
    }

    @Override
    public <T extends AbstractDerivedAttribute> boolean removeDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.remove(
                (MembershipDerivedAttribute) derivedAttribute);
    }

    @Override
    public Set<? extends AbstractDerivedAttribute> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public void setDerivedAttributes(
            Set<? extends AbstractDerivedAttribute> derivedAttributes) {

        this.derivedAttributes =
                (Set<MembershipDerivedAttribute>) derivedAttributes;
    }

    @Override
    public boolean addResource(Resource resource) {
        return false;
    }

    @Override
    public boolean removeResource(Resource resource) {
        return false;
    }

    @Override
    public Set<Resource> getResources() {
        return Collections.EMPTY_SET;
    }

    @Override
    public void setResources(Set<Resource> resources) {
    }
}
