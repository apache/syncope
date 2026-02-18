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
package org.apache.syncope.core.persistence.jpa.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import tools.jackson.core.type.TypeReference;

@Converter
public class AttrListConverter implements AttributeConverter<List<Attr>, String> {

    protected static final TypeReference<List<Attr>> TYPEREF = new TypeReference<List<Attr>>() {
    };

    @Override
    public String convertToDatabaseColumn(final List<Attr> attribute) {
        return Optional.ofNullable(attribute).map(POJOHelper::serialize).orElse(null);
    }

    @Override
    public List<Attr> convertToEntityAttribute(final String dbData) {
        return Optional.ofNullable(dbData).map(data -> POJOHelper.deserialize(data, TYPEREF)).orElseGet(ArrayList::new);
    }
}
