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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.TableGenerator;
import org.hibernate.annotations.Cascade;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.AbstractSchema;

@Entity
public class RoleAttribute extends AbstractAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE,
    generator = "SEQ_RoleAttribute")
    @TableGenerator(name = "SEQ_RoleAttribute", allocationSize = 20)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private SyncopeRole owner;

    @ManyToOne(fetch = FetchType.EAGER)
    private RoleSchema schema;

    @OneToMany(cascade = CascadeType.MERGE, mappedBy = "attribute")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<RoleAttributeValue> values;

    public RoleAttribute() {
        values = new ArrayList<RoleAttributeValue>();
    }

    @Override
    public Long getId() {
        return id;
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
    public <T extends AbstractAttributeValue> boolean addValue(
            T attributeValue) {

        return values.add((RoleAttributeValue) attributeValue);
    }

    @Override
    public <T extends AbstractAttributeValue> boolean removeValue(
            T attributeValue) {

        return values.remove((RoleAttributeValue) attributeValue);
    }

    @Override
    public <T extends AbstractAttributeValue> List<T> getValues() {
        return (List<T>) values;
    }

    @Override
    public <T extends AbstractAttributeValue> void setValues(
            List<T> attributeValues) {

        this.values = (List<RoleAttributeValue>) attributeValues;

    }
}
