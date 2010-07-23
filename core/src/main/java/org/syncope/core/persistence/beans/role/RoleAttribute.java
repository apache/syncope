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
package org.syncope.core.persistence.beans.role;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.AbstractSchema;

@Entity
public class RoleAttribute extends AbstractAttribute {

    @ManyToOne(fetch = FetchType.EAGER)
    private SyncopeRole owner;
    @ManyToOne(fetch = FetchType.EAGER)
    private RoleSchema schema;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER, mappedBy = "attribute")
    private Set<RoleAttributeValue> attributeValues;

    public RoleAttribute() {
        attributeValues = new HashSet<RoleAttributeValue>();
    }

    @Override
    public <T extends AbstractAttributable> T getOwner() {
        return (T) owner;
    }

    @Override
    public <T extends AbstractAttributable> void setOwner(T owner) {
        this.owner = (SyncopeRole) owner;
    }

    @Override
    public <T extends AbstractSchema> T getSchema() {
        return (T) schema;
    }

    @Override
    public <T extends AbstractSchema> void setSchema(T schema) {
        this.schema = (RoleSchema) schema;
    }

    @Override
    public <T extends AbstractAttributeValue> boolean addAttributeValue(
            T attributeValue) {

        return attributeValues.add((RoleAttributeValue) attributeValue);
    }

    @Override
    public <T extends AbstractAttributeValue> boolean removeAttributeValue(
            T attributeValue) {

        return attributeValues.remove((RoleAttributeValue) attributeValue);
    }

    @Override
    public Set<? extends AbstractAttributeValue> getAttributeValues() {
        return attributeValues;
    }

    @Override
    public void setAttributeValues(
            Set<? extends AbstractAttributeValue> attributeValues) {

        this.attributeValues = (Set<RoleAttributeValue>) attributeValues;
    }
}
