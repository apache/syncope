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
package org.apache.syncope.core.persistence.neo4j.entity.anyobject;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractPlainAttr;
import org.apache.syncope.core.spring.ApplicationContextProvider;

public class JSONAPlainAttr extends AbstractPlainAttr<AnyObject> implements APlainAttr {

    private static final long serialVersionUID = 806271775349587902L;

    /**
     * The owner of this attribute.
     */
    @JsonIgnore
    private Neo4jAnyObject owner;

    /**
     * The membership of this attribute; might be {@code NULL} if this attribute is not related to a membership.
     */
    @JsonProperty
    private String membership;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    private final List<JSONAPlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @JsonProperty
    private JSONAPlainAttrUniqueValue uniqueValue;

    @Override
    public AnyObject getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final AnyObject owner) {
        this.owner = (Neo4jAnyObject) owner;
    }

    @JsonIgnore
    @Override
    public AMembership getMembership() {
        return ApplicationContextProvider.getBeanFactory().getBean(AnyObjectDAO.class).findMembership(membership);
    }

    @Override
    public void setMembership(final AMembership membership) {
        this.membership = Optional.ofNullable(membership).map(AMembership::getKey).orElse(null);
    }

    @JsonGetter("membership")
    @Override
    public String getMembershipKey() {
        return membership;
    }

    @JsonSetter("membership")
    public void setMembership(final String membership) {
        this.membership = membership;
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        return values.add((JSONAPlainAttrValue) attrValue);
    }

    @Override
    public List<? extends APlainAttrValue> getValues() {
        return values;
    }

    @Override
    public JSONAPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @JsonIgnore
    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        this.uniqueValue = (JSONAPlainAttrUniqueValue) uniqueValue;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
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
        final JSONAPlainAttr other = (JSONAPlainAttr) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(membership, other.membership).
                append(values, other.values).
                append(uniqueValue, other.uniqueValue).
                build();
    }
}
