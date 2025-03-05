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
package org.apache.syncope.core.provisioning.api.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import org.identityconnectors.common.security.GuardedString;

public abstract class AbstractValueSerializer<T extends Object> extends JsonSerializer<T> {

    public static final String BYTE_ARRAY_PREFIX = "<binary>";

    public static final String BYTE_ARRAY_SUFFIX = "</binary>";

    protected void doSerialize(final List<Object> value, final JsonGenerator jgen) throws IOException {
        if (value == null) {
            jgen.writeNull();
        } else {
            jgen.writeStartArray();
            for (Object v : value) {
                if (v == null) {
                    jgen.writeNull();
                } else if (v instanceof GuardedString) {
                    jgen.writeObject(v);
                } else if (v instanceof final Integer i) {
                    jgen.writeNumber(i);
                } else if (v instanceof final Long l) {
                    jgen.writeNumber(l);
                } else if (v instanceof final Double aDouble) {
                    jgen.writeNumber(aDouble);
                } else if (v instanceof final Boolean b) {
                    jgen.writeBoolean(b);
                } else if (v instanceof final byte[] bytes) {
                    jgen.writeString(
                            BYTE_ARRAY_PREFIX
                            + Base64.getEncoder().encodeToString(bytes)
                            + BYTE_ARRAY_SUFFIX);
                } else {
                    jgen.writeString(v.toString());
                }
            }
            jgen.writeEndArray();
        }
    }
}
