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
package org.apache.syncope.core.persistence.neo4j.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractPlainAttrValue;

public class Neo4jUPlainAttrUniqueValue extends AbstractPlainAttrValue implements UPlainAttrUniqueValue {

    private static final long serialVersionUID = -4053996864791245312L;

    @JsonIgnore
    @NotNull
    private Neo4jUPlainAttr attr;

    @Override
    public UPlainAttr getAttr() {
        return attr;
    }

    @Override
    public void setAttr(final PlainAttr<?> attr) {
        checkType(attr, Neo4jUPlainAttr.class);
        this.attr = (Neo4jUPlainAttr) attr;
    }

    @JsonIgnore
    @Override
    public PlainSchema getSchema() {
        return getAttr() == null ? null : getAttr().getSchema();
    }

    @Override
    public void setSchema(final PlainSchema schema) {
        // nothing to do
    }
}
