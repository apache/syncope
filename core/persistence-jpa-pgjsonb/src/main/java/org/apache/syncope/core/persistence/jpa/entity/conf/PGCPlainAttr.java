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
package org.apache.syncope.core.persistence.jpa.entity.conf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.PGPlainAttr;
import org.apache.syncope.core.spring.ApplicationContextProvider;

@JsonIgnoreProperties("valuesAsStrings")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PGCPlainAttr extends AbstractPlainAttr<Conf> implements CPlainAttr, PGPlainAttr<Conf> {

    private static final long serialVersionUID = 806271775349587902L;

    /**
     * The owner of this attribute.
     */
    @JsonIgnore
    private PGJPAConf owner;

    @JsonProperty
    private String schema;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    private final List<PGCPlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @JsonProperty
    private PGCPlainAttrUniqueValue uniqueValue;

    @Override
    public Conf getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Conf owner) {
        checkType(owner, PGJPAConf.class);
        this.owner = (PGJPAConf) owner;
    }

    @JsonIgnore
    @Override
    public String getSchemaKey() {
        return schema;
    }

    @JsonIgnore
    @Override
    public JPAPlainSchema getSchema() {
        return (JPAPlainSchema) ApplicationContextProvider.getBeanFactory().getBean(PlainSchemaDAO.class).find(schema);
    }

    @Override
    public void setSchema(final PlainSchema schema) {
        if (schema != null) {
            this.schema = schema.getKey();
        }
    }

    @JsonSetter("schema")
    public void setSchema(final String schema) {
        this.schema = schema;
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        checkType(attrValue, PGCPlainAttrValue.class);
        return values.add((PGCPlainAttrValue) attrValue);
    }

    @Override
    public boolean add(final PlainAttrValue value) {
        return addForMultiValue(value);
    }

    @Override
    public List<? extends CPlainAttrValue> getValues() {
        return values;
    }

    @JsonIgnore
    public List<PGCPlainAttrValue> getPGValues() {
        return values;
    }

    @Override
    public PGCPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @JsonIgnore
    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(uniqueValue, PGCPlainAttrUniqueValue.class);
        this.uniqueValue = (PGCPlainAttrUniqueValue) uniqueValue;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(schema).
                append(values).
                append(uniqueValue).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PGCPlainAttr other = (PGCPlainAttr) obj;
        return new EqualsBuilder().
                append(schema, other.schema).
                append(values, other.values).
                append(uniqueValue, other.uniqueValue).
                build();
    }
}
