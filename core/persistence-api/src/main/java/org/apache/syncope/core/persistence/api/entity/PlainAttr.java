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
package org.apache.syncope.core.persistence.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PlainAttr implements Serializable {

    private static final long serialVersionUID = -9115431608821806124L;

    @NotNull
    private String schema;

    @JsonIgnore
    private PlainSchema plainSchema;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    private final List<PlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    private PlainAttrValue uniqueValue;

    /**
     * The membership of this attribute; might be {@code NULL} if this attribute is not related to a membership.
     */
    private String membership;

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    @JsonIgnore
    public void setPlainSchema(final PlainSchema plainSchema) {
        this.plainSchema = plainSchema;
        this.schema = plainSchema.getKey();
    }

    public void add(final PlainAttrValue attrValue) {
        values.add(attrValue);
    }

    public List<PlainAttrValue> getValues() {
        return values;
    }

    public PlainAttrValue getUniqueValue() {
        return uniqueValue;
    }

    public void setUniqueValue(final PlainAttrValue uniqueValue) {
        this.uniqueValue = uniqueValue;
    }

    @JsonIgnore
    protected PlainSchema fetchPlainSchema() {
        if (plainSchema == null) {
            if (schema == null) {
                throw new IllegalStateException("First set owner then schema and finally add values");
            }
            plainSchema = ApplicationContextProvider.getApplicationContext().getBean(PlainSchemaDAO.class).
                    findById(schema).
                    orElseThrow(() -> new NotFoundException("PlainSchema " + schema));
        }

        return plainSchema;
    }

    public void add(final PlainAttrValidationManager validator, final String value) {
        PlainAttrValue attrValue = new PlainAttrValue();
        attrValue.setAttr(this);

        fetchPlainSchema();

        validator.validate(plainSchema, value, attrValue);

        if (plainSchema.isUniqueConstraint()) {
            setUniqueValue(attrValue);
        } else {
            if (!plainSchema.isMultivalue()) {
                values.clear();
            }
            values.add(attrValue);
        }
    }

    @JsonIgnore
    public List<String> getValuesAsStrings() {
        List<String> result;
        if (getUniqueValue() == null) {
            result = getValues().stream().map(PlainAttrValue::getValueAsString).toList();
        } else {
            result = List.of(getUniqueValue().getValueAsString());
        }

        return Collections.unmodifiableList(result);
    }

    public String getMembership() {
        return membership;
    }

    public void setMembership(final String membership) {
        this.membership = membership;
    }

    @JsonIgnore
    public boolean isValid() {
        boolean validSchema = false;
        try {
            validSchema = fetchPlainSchema() != null;
        } catch (Exception e) {
            // ignore
        }
        return validSchema && (!values.isEmpty() || uniqueValue != null);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(schema).
                append(values).
                append(uniqueValue).
                append(membership).
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
        @SuppressWarnings("unchecked")
        final PlainAttr other = (PlainAttr) obj;
        return new EqualsBuilder().
                append(schema, other.schema).
                append(values, other.values).
                append(uniqueValue, other.uniqueValue).
                append(membership, other.membership).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(schema).
                append(values).
                append(uniqueValue).
                append(membership).
                build();
    }
}
