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
package org.apache.syncope.core.persistence.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.spring.ApplicationContextProvider;

@JsonIgnoreProperties("valuesAsStrings")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class AbstractPlainAttr<O extends Any<?>> implements PlainAttr<O> {

    private static final long serialVersionUID = -9115431608821806124L;

    @JsonIgnore
    @NotNull
    private String schemaKey;

    @JsonIgnore
    @Override
    public String getKey() {
        return null;
    }

    @JsonIgnore
    @Override
    public String getSchemaKey() {
        return schemaKey;
    }

    @JsonIgnore
    @Override
    public Neo4jPlainSchema getSchema() {
        return Optional.ofNullable(schemaKey).
                flatMap(s -> ApplicationContextProvider.getBeanFactory().getBean(PlainSchemaDAO.class).findById(s)).
                map(Neo4jPlainSchema.class::cast).
                orElse(null);
    }

    @JsonIgnore
    @Override
    public void setSchema(final PlainSchema schema) {
        if (schema != null) {
            this.schemaKey = schema.getKey();
        }
    }

    @JsonIgnore
    public void setSchema(final String schemaKey) {
        this.schemaKey = schemaKey;
    }

    protected abstract boolean addForMultiValue(PlainAttrValue attrValue);

    @Override
    public void add(final PlainAttrValue attrValue) {
        addForMultiValue(attrValue);
    }

    private void checkNonNullSchema() {
        if (getSchema() == null) {
            throw new IllegalStateException("First set owner then schema and finally add values");
        }
    }

    @Override
    public void add(final PlainAttrValidationManager validator, final String value, final PlainAttrValue attrValue) {
        checkNonNullSchema();

        attrValue.setAttr(this);
        validator.validate(getSchema(), value, attrValue);

        if (getSchema().isUniqueConstraint()) {
            setUniqueValue((PlainAttrUniqueValue) attrValue);
        } else {
            if (!getSchema().isMultivalue()) {
                getValues().clear();
            }
            addForMultiValue(attrValue);
        }
    }

    @Override
    public void add(final PlainAttrValidationManager validator, final String value, final AnyUtils anyUtils) {
        checkNonNullSchema();

        PlainAttrValue attrValue;
        if (getSchema().isUniqueConstraint()) {
            attrValue = anyUtils.newPlainAttrUniqueValue();
            ((PlainAttrUniqueValue) attrValue).setSchema(getSchema());
        } else {
            attrValue = anyUtils.newPlainAttrValue();
        }

        add(validator, value, attrValue);
    }

    @Override
    public List<String> getValuesAsStrings() {
        List<String> result;
        if (getUniqueValue() == null) {
            result = getValues().stream().map(PlainAttrValue::getValueAsString).toList();
        } else {
            result = List.of(getUniqueValue().getValueAsString());
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(schemaKey).
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
        final AbstractPlainAttr<O> other = (AbstractPlainAttr<O>) obj;
        return new EqualsBuilder().
                append(schemaKey, other.schemaKey).
                build();
    }
}
