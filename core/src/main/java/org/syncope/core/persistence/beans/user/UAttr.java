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
package org.syncope.core.persistence.beans.user;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractSchema;

/**
 * User attribute.
 */
@Entity
public class UAttr extends AbstractAttr {

    private static final long serialVersionUID = 6333601983691157406L;

    /**
     * Auto-generated id for this table.
     */
    @Id
    private Long id;

    /**
     * The owner of this attribute.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private SyncopeUser owner;

    /**
     * The schema of this attribute.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private USchema schema;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    @OneToMany(cascade = CascadeType.MERGE, orphanRemoval = true,
    mappedBy = "attribute")
    @Valid
    private List<UAttrValue> values;

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "attribute")
    @Valid
    private UAttrUniqueValue uniqueValue;

    /**
     * Default constructor.
     */
    public UAttr() {
        super();
        values = new ArrayList<UAttrValue>();
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
    public <T extends AbstractAttributable> void setOwner(final T owner) {
        this.owner = (SyncopeUser) owner;
    }

    @Override
    public <T extends AbstractSchema> T getSchema() {
        return (T) schema;
    }

    @Override
    public <T extends AbstractSchema> void setSchema(T schema) {
        this.schema = (USchema) schema;
    }

    @Override
    public <T extends AbstractAttrValue> boolean addValue(
            final T attributeValue) {

        return values.add((UAttrValue) attributeValue);
    }

    @Override
    public <T extends AbstractAttrValue> boolean removeValue(
            final T attributeValue) {

        return values.remove((UAttrValue) attributeValue);
    }

    @Override
    public <T extends AbstractAttrValue> List<T> getValues() {
        return (List<T>) values;
    }

    @Override
    public <T extends AbstractAttrValue> void setValues(
            final List<T> attributeValues) {

        this.values.clear();
        if (attributeValues != null && !attributeValues.isEmpty()) {
            for (T mav : attributeValues) {
                mav.setAttribute(this);
            }
            this.values.addAll((List<UAttrValue>) attributeValues);
        }
    }

    @Override
    public <T extends AbstractAttrValue> T getUniqueValue() {
        return (T) uniqueValue;
    }

    @Override
    public <T extends AbstractAttrValue> void setUniqueValue(
            final T uniqueAttributeValue) {

        this.uniqueValue = (UAttrUniqueValue) uniqueAttributeValue;
    }
}
