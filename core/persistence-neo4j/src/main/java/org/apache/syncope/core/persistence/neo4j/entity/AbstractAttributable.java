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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.common.validation.AttributableCheck;
import org.springframework.data.neo4j.core.schema.PostLoad;

@AttributableCheck
public abstract class AbstractAttributable extends AbstractGeneratedKeyNode implements Attributable {

    private static final long serialVersionUID = 5653313070976526466L;

    protected abstract Map<String, PlainAttr> plainAttrs();

    @Override
    public boolean add(final PlainAttr attr) {
        return plainAttrs().put(attr.getSchema(), attr) != null;
    }

    @Override
    public boolean remove(final PlainAttr attr) {
        return plainAttrs().put(attr.getSchema(), null) != null;
    }

    @Override
    public List<PlainAttr> getPlainAttrs() {
        return plainAttrs().entrySet().stream().
                filter(e -> e.getValue() != null).
                sorted(Comparator.comparing(Map.Entry::getKey)).
                map(Map.Entry::getValue).toList();
    }

    @Override
    public Optional<PlainAttr> getPlainAttr(final String plainSchema) {
        return Optional.ofNullable(plainAttrs().get(plainSchema));
    }

    protected void doComplete(final Map<String, PlainAttr> plainAttrs) {
        for (var itor = plainAttrs.entrySet().iterator(); itor.hasNext();) {
            var entry = itor.next();
            Optional.ofNullable(entry.getValue()).ifPresent(attr -> {
                attr.setSchema(entry.getKey());
                if (attr.getSchema() == null) {
                    itor.remove();
                } else {
                    attr.getValues().forEach(value -> value.setAttr(attr));
                    Optional.ofNullable(attr.getUniqueValue()).ifPresent(value -> value.setAttr(attr));
                }
            });
        }
    }

    @PostLoad
    public void completePlainAttrs() {
        doComplete(plainAttrs());
    }
}
