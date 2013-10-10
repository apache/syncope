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
package org.apache.syncope.core.persistence.beans.membership;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractNormalSchema;

@Entity
public class MAttr extends AbstractAttr {

    private static final long serialVersionUID = 3755864809152866489L;

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private Membership owner;

    @Column(nullable = false)
    @OneToOne(cascade = CascadeType.MERGE)
    private MAttrTemplate template;

    @OneToMany(cascade = CascadeType.MERGE, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private List<MAttrValue> values;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "attribute")
    @Valid
    private MAttrUniqueValue uniqueValue;

    public MAttr() {
        super();
        values = new ArrayList<MAttrValue>();
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
        if (!(owner instanceof Membership)) {
            throw new ClassCastException("owner is expected to be typed Membership: " + owner.getClass().getName());
        }
        this.owner = (Membership) owner;
    }

    public MAttrTemplate getTemplate() {
        return template;
    }

    public void setTemplate(final MAttrTemplate template) {
        this.template = template;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractNormalSchema> T getSchema() {
        return template == null ? null : (T) template.getSchema();
    }

    @Override
    public <T extends AbstractAttrValue> boolean addValue(final T attributeValue) {
        if (!(attributeValue instanceof MAttrValue)) {
            throw new ClassCastException("attributeValue is expected to be typed MAttrValue: " + attributeValue.
                    getClass().getName());
        }
        attributeValue.setAttribute(this);
        return values.add((MAttrValue) attributeValue);
    }

    @Override
    public <T extends AbstractAttrValue> boolean removeValue(final T attributeValue) {
        if (!(attributeValue instanceof MAttrValue)) {
            throw new ClassCastException("attributeValue is expected to be typed MAttrValue: " + attributeValue.
                    getClass().getName());
        }
        boolean result = values.remove((MAttrValue) attributeValue);
        attributeValue.setAttribute(null);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractAttrValue> List<T> getValues() {
        return (List<T>) values;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractAttrValue> void setValues(final List<T> attributeValues) {
        this.values.clear();
        if (attributeValues != null && !attributeValues.isEmpty()) {
            for (T mav : attributeValues) {
                mav.setAttribute(this);
            }
            this.values.addAll((List<MAttrValue>) attributeValues);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractAttrValue> T getUniqueValue() {
        return (T) uniqueValue;
    }

    @Override
    public <T extends AbstractAttrValue> void setUniqueValue(final T uniqueAttributeValue) {
        if (!(uniqueAttributeValue instanceof MAttrUniqueValue)) {
            throw new ClassCastException("uniqueAttributeValue is expected to be typed MAttrUniqueValue: "
                    + uniqueAttributeValue.getClass().getName());
        }
        this.uniqueValue = (MAttrUniqueValue) uniqueAttributeValue;
    }
}
