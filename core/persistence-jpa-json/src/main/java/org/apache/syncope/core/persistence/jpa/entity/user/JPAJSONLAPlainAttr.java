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
import org.apache.syncope.core.persistence.api.entity.JSONLAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.apache.syncope.core.spring.ApplicationContextProvider;

@JsonIgnoreProperties("valuesAsStrings")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JPAJSONLAPlainAttr extends AbstractPlainAttr<User> implements JSONLAPlainAttr {

    private static final long serialVersionUID = -7712812886044037467L;

    /**
     * The owner of this attribute.
     */
    @JsonIgnore
    private JPAJSONUser owner;

    @JsonIgnore
    private JPAJSONLinkedAccount account;

    @JsonProperty
    private String schema;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    private final List<JPAJSONLAPlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @JsonProperty
    private JPAJSONLAPlainAttrUniqueValue uniqueValue;

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final User owner) {
        checkType(owner, JPAJSONUser.class);
        this.owner = (JPAJSONUser) owner;
    }

    @Override
    public LinkedAccount getAccount() {
        return account;
    }

    @Override
    public void setAccount(final LinkedAccount account) {
        checkType(account, JPAJSONLinkedAccount.class);
        this.account = (JPAJSONLinkedAccount) account;
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
        checkType(attrValue, JPAJSONLAPlainAttrValue.class);
        return values.add((JPAJSONLAPlainAttrValue) attrValue);
    }

    @Override
    public boolean add(final PlainAttrValue value) {
        return addForMultiValue(value);
    }

    @Override
    public List<? extends LAPlainAttrValue> getValues() {
        return values;
    }

    @JsonIgnore
    public List<JPAJSONLAPlainAttrValue> getPGValues() {
        return values;
    }

    @Override
    public JPAJSONLAPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @JsonIgnore
    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(uniqueValue, JPAJSONLAPlainAttrUniqueValue.class);
        this.uniqueValue = (JPAJSONLAPlainAttrUniqueValue) uniqueValue;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(schema).
                append(account).
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
        final JPAJSONLAPlainAttr other = (JPAJSONLAPlainAttr) obj;
        return new EqualsBuilder().
                append(schema, other.schema).
                append(account, other.account).
                append(values, other.values).
                append(uniqueValue, other.uniqueValue).
                build();
    }
}
