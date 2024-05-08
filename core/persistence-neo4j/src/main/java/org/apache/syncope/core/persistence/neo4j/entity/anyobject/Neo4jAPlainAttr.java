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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jPlainAttr;
import org.apache.syncope.core.spring.ApplicationContextProvider;

public class Neo4jAPlainAttr extends AbstractPlainAttr<AnyObject> implements APlainAttr, Neo4jPlainAttr<AnyObject> {

    private static final long serialVersionUID = 806271775349587902L;

    /**
     * The owner of this attribute.
     */
    @JsonIgnore
    private Neo4jAnyObject owner;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    private final List<Neo4jAPlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @JsonProperty
    private Neo4jAPlainAttrUniqueValue uniqueValue;

    @Override
    public AnyObject getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final AnyObject owner) {
        checkType(owner, Neo4jAnyObject.class);
        this.owner = (Neo4jAnyObject) owner;
    }

    @JsonIgnore
    @Override
    public AMembership getMembership() {
        return ApplicationContextProvider.getBeanFactory().getBean(AnyObjectDAO.class).findMembership(membershipKey);
    }

    @JsonIgnore
    @Override
    public void setMembership(final AMembership membership) {
        checkType(membership, Neo4jAMembership.class);
        if (membership != null) {
            this.membershipKey = membership.getKey();
        }
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        checkType(attrValue, Neo4jAPlainAttrValue.class);
        return values.add((Neo4jAPlainAttrValue) attrValue);
    }

    @Override
    public boolean add(final PlainAttrValue value) {
        return addForMultiValue(value);
    }

    @Override
    public List<? extends APlainAttrValue> getValues() {
        return values;
    }

    @Override
    public Neo4jAPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @JsonIgnore
    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(uniqueValue, Neo4jAPlainAttrUniqueValue.class);
        this.uniqueValue = (Neo4jAPlainAttrUniqueValue) uniqueValue;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(schemaKey).
                append(membershipKey).
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
        final Neo4jAPlainAttr other = (Neo4jAPlainAttr) obj;
        return new EqualsBuilder().
                append(schemaKey, other.schemaKey).
                append(membershipKey, other.membershipKey).
                append(values, other.values).
                append(uniqueValue, other.uniqueValue).
                build();
    }
}
