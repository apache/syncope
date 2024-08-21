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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;

public class JSONLAPlainAttr extends AbstractPlainAttr<User> implements LAPlainAttr {

    private static final long serialVersionUID = 7827533741035423694L;

    /**
     * The owner of this attribute.
     */
    @JsonIgnore
    private JPAUser owner;

    @JsonIgnore
    private JPALinkedAccount account;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    private List<JSONLAPlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @JsonProperty
    private JSONLAPlainAttrUniqueValue uniqueValue;

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final User owner) {
        this.owner = (JPAUser) owner;
    }

    @Override
    public LinkedAccount getAccount() {
        return account;
    }

    @Override
    public void setAccount(final LinkedAccount account) {
        this.account = (JPALinkedAccount) account;
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        return values.add((JSONLAPlainAttrValue) attrValue);
    }

    @Override
    public List<? extends LAPlainAttrValue> getValues() {
        return values;
    }

    @Override
    public JSONLAPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @JsonIgnore
    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        this.uniqueValue = (JSONLAPlainAttrUniqueValue) uniqueValue;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
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
        final JSONLAPlainAttr other = (JSONLAPlainAttr) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(account, other.account).
                append(values, other.values).
                append(uniqueValue, other.uniqueValue).
                build();
    }
}
