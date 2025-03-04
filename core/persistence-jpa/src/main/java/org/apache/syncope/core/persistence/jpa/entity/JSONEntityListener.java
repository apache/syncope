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
package org.apache.syncope.core.persistence.jpa.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

public abstract class JSONEntityListener<A extends Any> {

    protected static final TypeReference<List<PlainAttr>> TYPEREF = new TypeReference<List<PlainAttr>>() {
    };

    protected List<PlainAttr> getAttrs(final String plainAttrsJSON) {
        return POJOHelper.deserialize(plainAttrsJSON, TYPEREF);
    }

    protected void json2list(final AbstractAttributable entity, final boolean clearFirst) {
        if (clearFirst) {
            entity.getPlainAttrsList().clear();
        }
        if (entity.getPlainAttrsJSON() != null) {
            getAttrs(entity.getPlainAttrsJSON()).stream().filter(PlainAttr::isValid).peek(attr -> {
                attr.getValues().forEach(value -> value.setAttr(attr));
                Optional.ofNullable(attr.getUniqueValue()).ifPresent(value -> value.setAttr(attr));
            }).forEach(attr -> entity.add(attr));
        }
    }
}
