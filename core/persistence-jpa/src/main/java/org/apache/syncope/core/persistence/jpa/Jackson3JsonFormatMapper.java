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
package org.apache.syncope.core.persistence.jpa;

import java.lang.reflect.Type;
import java.util.List;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import org.hibernate.type.format.FormatMapperCreationContext;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

public final class Jackson3JsonFormatMapper extends AbstractJsonFormatMapper {

    public static final String SHORT_NAME = "jackson3";

    private final JsonMapper jsonMapper;

    public Jackson3JsonFormatMapper() {
        this(MapperBuilder.findModules(Jackson3JsonFormatMapper.class.getClassLoader()));
    }

    public Jackson3JsonFormatMapper(final FormatMapperCreationContext creationContext) {
        this(creationContext.
                getBootstrapContext().
                getClassLoaderService().
                <List<JacksonModule>>workWithClassLoader(MapperBuilder::findModules));
    }

    private Jackson3JsonFormatMapper(final List<JacksonModule> modules) {
        this(JsonMapper.builderWithJackson2Defaults().addModules(modules).build());
    }

    public Jackson3JsonFormatMapper(final JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public <T> void writeToTarget(
            final T value,
            final JavaType<T> javaType,
            final Object target,
            final WrapperOptions options)
            throws JacksonException {

        jsonMapper.writerFor(jsonMapper.constructType(javaType.getJavaType())).
                writeValue((JsonGenerator) target, value);
    }

    @Override
    public <T> T readFromSource(
            final JavaType<T> javaType,
            final Object source,
            final WrapperOptions options) throws JacksonException {

        return jsonMapper.readValue((JsonParser) source, jsonMapper.constructType(javaType.getJavaType()));
    }

    @Override
    public boolean supportsSourceType(final Class<?> sourceType) {
        return JsonParser.class.isAssignableFrom(sourceType);
    }

    @Override
    public boolean supportsTargetType(final Class<?> targetType) {
        return JsonGenerator.class.isAssignableFrom(targetType);
    }

    @Override
    public <T> T fromString(final CharSequence charSequence, final Type type) {
        try {
            return jsonMapper.readValue(charSequence.toString(), jsonMapper.constructType(type));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Could not deserialize string to java type: " + type, e);
        }
    }

    @Override
    public <T> String toString(final T value, final Type type) {
        try {
            return jsonMapper.writerFor(jsonMapper.constructType(type)).writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Could not serialize object of java type: " + type, e);
        }
    }
}
