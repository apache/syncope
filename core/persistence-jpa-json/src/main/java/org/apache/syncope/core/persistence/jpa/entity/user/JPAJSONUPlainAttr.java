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
package org.apache.syncope.core.persistence.jpa.entity.user;

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
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.JSONPlainAttr;

@JsonIgnoreProperties("valuesAsStrings")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JPAJSONUPlainAttr extends AbstractPlainAttr<User> implements UPlainAttr, JSONPlainAttr<User> {

    private static final long serialVersionUID = 806271775349587902L;

    /**
     * The owner of this attribute.
     */
    @JsonIgnore
    private JPAJSONUser owner;

    @JsonProperty
    private String schema;

    /**
     * The membership of this attribute; might be {@code NULL} if this attribute is not related to a membership.
     */
    @JsonProperty
    private String membership;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    private final List<JPAJSONUPlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @JsonProperty
    private JPAJSONUPlainAttrUniqueValue uniqueValue;

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final User owner) {
        checkType(owner, JPAJSONUser.class);
        this.owner = (JPAJSONUser) owner;
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

    @JsonIgnore
    public String getMembershipKey() {
        return membership;
    }

    @JsonIgnore
    @Override
    public UMembership getMembership() {
        return ApplicationContextProvider.getBeanFactory().getBean(UserDAO.class).findMembership(membership);
    }

    @Override
    public void setMembership(final UMembership membership) {
        if (membership != null) {
            this.membership = membership.getKey();
        }
    }

    @JsonSetter("membership")
    public void setMembership(final String membership) {
        this.membership = membership;
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        checkType(attrValue, JPAJSONUPlainAttrValue.class);
        return values.add((JPAJSONUPlainAttrValue) attrValue);
    }

    @Override
    public boolean add(final PlainAttrValue value) {
        return addForMultiValue(value);
    }

    @Override
    public List<? extends UPlainAttrValue> getValues() {
        return values;
    }

    @JsonIgnore
    public List<JPAJSONUPlainAttrValue> getPGValues() {
        return values;
    }

    @Override
    public JPAJSONUPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @JsonIgnore
    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(uniqueValue, JPAJSONUPlainAttrUniqueValue.class);
        this.uniqueValue = (JPAJSONUPlainAttrUniqueValue) uniqueValue;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(schema).
                append(membership).
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
        final JPAJSONUPlainAttr other = (JPAJSONUPlainAttr) obj;
        return new EqualsBuilder().
                append(schema, other.schema).
                append(membership, other.membership).
                append(values, other.values).
                append(uniqueValue, other.uniqueValue).
                build();
    }
}
