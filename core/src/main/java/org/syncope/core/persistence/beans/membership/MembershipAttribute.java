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

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.AbstractSchema;

@Entity
public class MembershipAttribute extends AbstractAttribute {

    @ManyToOne(fetch = FetchType.EAGER)
    private Membership owner;
    @ManyToOne(fetch = FetchType.EAGER)
    private MembershipSchema schema;
    @OneToMany(cascade = javax.persistence.CascadeType.ALL,
    fetch = FetchType.EAGER, mappedBy = "attribute")
    @Cascade(CascadeType.DELETE_ORPHAN)
    private Set<MembershipAttributeValue> attributeValues;

    public MembershipAttribute() {
        attributeValues = new HashSet<MembershipAttributeValue>();
    }

    @Override
    public <T extends AbstractAttributable> T getOwner() {
        return (T) owner;
    }

    @Override
    public <T extends AbstractAttributable> void setOwner(T owner) {
        this.owner = (Membership) owner;
    }

    @Override
    public <T extends AbstractSchema> T getSchema() {
        return (T) schema;
    }

    @Override
    public <T extends AbstractSchema> void setSchema(T schema) {
        this.schema = (MembershipSchema) schema;
    }

    @Override
    public <T extends AbstractAttributeValue> boolean addAttributeValue(
            T attributeValue) {

        return attributeValues.add((MembershipAttributeValue) attributeValue);
    }

    @Override
    public <T extends AbstractAttributeValue> boolean removeAttributeValue(
            T attributeValue) {

        return attributeValues.remove((MembershipAttributeValue) attributeValue);
    }

    @Override
    public Set<? extends AbstractAttributeValue> getAttributeValues() {
        return attributeValues;
    }

    @Override
    public void setAttributeValues(
            Set<? extends AbstractAttributeValue> attributeValues) {

        this.attributeValues = (Set<MembershipAttributeValue>) attributeValues;
    }
}
