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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
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
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {"syncopeUser_id", "syncopeRole_id"}))
public class Membership extends AbstractAttributable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    private SyncopeUser syncopeUser;
    @ManyToOne
    private SyncopeRole syncopeRole;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<MembershipAttribute> attributes;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<MembershipDerivedAttribute> derivedAttributes;

    public Membership() {
        attributes = new ArrayList<MembershipAttribute>();
        derivedAttributes = new ArrayList<MembershipDerivedAttribute>();
        targetResources = Collections.EMPTY_SET;
    }

    @Override
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
    public List<? extends AbstractAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(List<? extends AbstractAttribute> attributes) {
        this.attributes = (List<MembershipAttribute>) attributes;
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
    public List<? extends AbstractDerivedAttribute> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public void setDerivedAttributes(
            List<? extends AbstractDerivedAttribute> derivedAttributes) {

        this.derivedAttributes =
                (List<MembershipDerivedAttribute>) derivedAttributes;
    }

    @Override
    public boolean addTargetResource(TargetResource resource) {
        return false;
    }

    @Override
    public boolean removeTargetResource(TargetResource resource) {
        return false;
    }

    @Override
    public Set<TargetResource> getTargetResources() {
        return Collections.EMPTY_SET;
    }

    @Override
    public void setResources(Set<TargetResource> resources) {
    }

    @Override
    public String toString() {
        return "Membership[" + "id=" + id
                + ", " + syncopeUser
                + ", " + syncopeRole + ']';
    }
}
