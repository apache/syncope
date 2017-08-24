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
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Base64;
import org.identityconnectors.framework.common.objects.SyncToken;

class SyncTokenSerializer extends JsonSerializer<SyncToken> {

    @Override
    public void serialize(final SyncToken source, final JsonGenerator jgen, final SerializerProvider sp)
            throws IOException {

        jgen.writeStartObject();

        jgen.writeFieldName("value");

        if (source.getValue() == null) {
            jgen.writeNull();
        } else if (source.getValue() instanceof Boolean) {
            jgen.writeBoolean((Boolean) source.getValue());
        } else if (source.getValue() instanceof Double) {
            jgen.writeNumber((Double) source.getValue());
        } else if (source.getValue() instanceof Long) {
            jgen.writeNumber((Long) source.getValue());
        } else if (source.getValue() instanceof Integer) {
            jgen.writeNumber((Integer) source.getValue());
        } else if (source.getValue() instanceof byte[]) {
            jgen.writeString(Base64.getEncoder().encodeToString((byte[]) source.getValue()));
        } else {
            jgen.writeString(source.getValue().toString());
        }

        jgen.writeEndObject();
    }

}
