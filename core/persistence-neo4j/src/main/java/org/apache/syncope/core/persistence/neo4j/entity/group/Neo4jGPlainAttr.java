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
package org.apache.syncope.core.persistence.neo4j.entity.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractPlainAttr;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jPlainAttr;

public class Neo4jGPlainAttr extends AbstractPlainAttr<Group> implements GPlainAttr, Neo4jPlainAttr<Group> {

    private static final long serialVersionUID = 806271775349587902L;

    /**
     * The owner of this attribute.
     */
    @JsonIgnore
    private Neo4jGroup owner;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    private final List<Neo4jGPlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @JsonProperty
    private Neo4jGPlainAttrUniqueValue uniqueValue;

    @Override
    public Group getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Group owner) {
        checkType(owner, Neo4jGroup.class);
        this.owner = (Neo4jGroup) owner;
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        checkType(attrValue, Neo4jGPlainAttrValue.class);
        return values.add((Neo4jGPlainAttrValue) attrValue);
    }

    @Override
    public boolean add(final PlainAttrValue value) {
        return addForMultiValue(value);
    }

    @Override
    public List<? extends GPlainAttrValue> getValues() {
        return values;
    }

    @Override
    public Neo4jGPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @JsonIgnore
    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(uniqueValue, Neo4jGPlainAttrUniqueValue.class);
        this.uniqueValue = (Neo4jGPlainAttrUniqueValue) uniqueValue;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(schemaKey).
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
        final Neo4jGPlainAttr other = (Neo4jGPlainAttr) obj;
        return new EqualsBuilder().
                append(schemaKey, other.schemaKey).
                append(values, other.values).
                append(uniqueValue, other.uniqueValue).
                build();
    }
}
