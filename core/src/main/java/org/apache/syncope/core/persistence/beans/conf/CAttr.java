/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.beans.conf;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractNormalSchema;

/**
 * User attribute.
 */
@Entity
public class CAttr extends AbstractAttr {

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
    private SyncopeConf owner;

    /**
     * The schema of this attribute.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "schema_name")
    private CSchema schema;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    @OneToMany(cascade = CascadeType.MERGE, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private List<CAttrValue> values;

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "attribute")
    @Valid
    private CAttrUniqueValue uniqueValue;

    /**
     * Default constructor.
     */
    public CAttr() {
        super();
        values = new ArrayList<CAttrValue>();
    }

    @Override
    public Long getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractAttributable> T getOwner() {
        return (T) owner;
    }

    @Override
    public <T extends AbstractAttributable> void setOwner(final T owner) {
        if (!(owner instanceof SyncopeConf)) {
            throw new ClassCastException("owner is expected to be typed SyncopeConf: " + owner.getClass().getName());
        }
        this.owner = (SyncopeConf) owner;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractNormalSchema> T getSchema() {
        return (T) schema;
    }

    public void setSchema(final CSchema schema) {
        this.schema = schema;
    }

    @Override
    public <T extends AbstractAttrValue> boolean addValue(final T attributeValue) {
        if (!(attributeValue instanceof CAttrValue)) {
            throw new ClassCastException("attributeValue is expected to be typed CAttrValue: " + attributeValue.
                    getClass().getName());
        }
        return values.add((CAttrValue) attributeValue);
    }

    @Override
    public <T extends AbstractAttrValue> boolean removeValue(final T attributeValue) {
        if (!(attributeValue instanceof CAttrValue)) {
            throw new ClassCastException("attributeValue is expected to be typed UAttrValue: " + attributeValue.
                    getClass().getName());
        }
        return values.remove((CAttrValue) attributeValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractAttrValue> List<T> getValues() {
        return (List<T>) values;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractAttrValue> T getUniqueValue() {
        return (T) uniqueValue;
    }

    @Override
    public <T extends AbstractAttrValue> void setUniqueValue(final T uniqueAttributeValue) {
        if (!(uniqueAttributeValue instanceof CAttrUniqueValue)) {
            throw new ClassCastException("uniqueAttributeValue is expected to be typed CAttrUniqueValue: "
                    + uniqueAttributeValue.getClass().getName());
        }
        this.uniqueValue = (CAttrUniqueValue) uniqueAttributeValue;
    }
}
