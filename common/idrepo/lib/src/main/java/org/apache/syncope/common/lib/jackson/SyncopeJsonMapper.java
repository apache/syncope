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
package org.apache.syncope.common.lib.jackson;

import java.io.DataOutput;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.util.TokenBuffer;

/**
 * Jackson ObjectMapper that unwraps singleton map values and enable default
 * typing for handling abstract types serialization.
 */
public class SyncopeJsonMapper extends JsonMapper {

    private static final long serialVersionUID = -317191546835195103L;

    public SyncopeJsonMapper() {
        super(JsonMapper.builder().findAndAddModules().enable(MapperFeature.USE_GETTERS_AS_SETTERS));
    }

    /**
     * Unwraps the given value if it implements the Map interface and contains only a single entry, otherwise the
     * value is returned unmodified.
     *
     * @param value the potential Map to unwrap
     * @return the unwrapped map or the original value
     */
    protected Object unwrapMap(final Object value) {
        if (value instanceof final Map<?, ?> map) {
            if (map.size() == 1) {
                return map.values().iterator().next();
            }
        }

        return value;
    }

    @Override
    public void writeValue(final DataOutput out, final Object value) throws JacksonException {
        super.writeValue(out, unwrapMap(value));
    }

    @Override
    public void writeValue(final Path path, final Object value) throws JacksonException {
        super.writeValue(path, unwrapMap(value));
    }

    @Override
    public TokenBuffer writeValueIntoBuffer(final Object value) throws JacksonException {
        return super.writeValueIntoBuffer(unwrapMap(value));
    }

    @Override
    public void writeValue(final JsonGenerator jgen, final Object value) throws JacksonException {
        super.writeValue(jgen, unwrapMap(value));
    }

    @Override
    public void writeValue(final File resultFile, final Object value) throws JacksonException {
        super.writeValue(resultFile, unwrapMap(value));
    }

    @Override
    public void writeValue(final OutputStream out, final Object value) throws JacksonException {
        super.writeValue(out, unwrapMap(value));
    }

    @Override
    public void writeValue(final Writer writer, final Object value) throws JacksonException {
        super.writeValue(writer, unwrapMap(value));
    }

    @Override
    public byte[] writeValueAsBytes(final Object value) throws JacksonException {
        return super.writeValueAsBytes(unwrapMap(value));
    }

    @Override
    public String writeValueAsString(final Object value) throws JacksonException {
        return super.writeValueAsString(unwrapMap(value));
    }
}
