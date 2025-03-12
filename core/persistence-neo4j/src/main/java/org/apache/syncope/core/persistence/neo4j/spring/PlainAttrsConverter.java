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
package org.apache.syncope.core.persistence.neo4j.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.value.NullValue;
import org.neo4j.driver.internal.value.StringValue;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;

public class PlainAttrsConverter implements Neo4jPersistentPropertyToMapConverter<String, Map<String, PlainAttr>> {

    @Override
    public Map<String, Value> decompose(
            final Map<String, PlainAttr> property,
            final Neo4jConversionService neo4jConversionService) {

        if (property == null) {
            return Map.of();
        }

        Map<String, Value> decomposed = new HashMap<>(property.size());
        property.forEach((k, v) -> Optional.ofNullable(v).
                flatMap(n -> Optional.ofNullable(POJOHelper.serialize(n))).
                ifPresentOrElse(
                        s -> decomposed.put(k, new StringValue(s)),
                        () -> decomposed.put(k, NullValue.NULL)));
        return decomposed;
    }

    @Override
    public Map<String, PlainAttr> compose(
            final Map<String, Value> source,
            final Neo4jConversionService neo4jConversionService) {

        Map<String, PlainAttr> composed = new HashMap<>(source.size());
        source.forEach((k, v) -> {
            if (v instanceof StringValue) {
                PlainAttr attr = POJOHelper.deserialize(v.asString(), PlainAttr.class);
                attr.setSchema(k);
                if (attr.isValid()) {
                    attr.getValues().forEach(value -> value.setAttr(attr));
                    Optional.ofNullable(attr.getUniqueValue()).ifPresent(value -> value.setAttr(attr));

                    composed.put(k, attr);
                }
            }
        });
        return composed;
    }
}
