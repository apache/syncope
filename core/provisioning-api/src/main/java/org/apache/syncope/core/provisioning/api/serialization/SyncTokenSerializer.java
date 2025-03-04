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

        if (source.getValue() == null) {
            jgen.writeNullField("value");
        } else if (source.getValue() instanceof final Boolean b) {
            jgen.writeStringField("type", Boolean.class.getSimpleName());
            jgen.writeBooleanField("value", b);
        } else if (source.getValue() instanceof final Double v) {
            jgen.writeStringField("type", Double.class.getSimpleName());
            jgen.writeNumberField("value", v);
        } else if (source.getValue() instanceof final Long l) {
            jgen.writeStringField("type", Long.class.getSimpleName());
            jgen.writeNumberField("value", l);
        } else if (source.getValue() instanceof final Integer i) {
            jgen.writeStringField("type", Integer.class.getSimpleName());
            jgen.writeNumberField("value", i);
        } else if (source.getValue() instanceof final byte[] bytes) {
            jgen.writeStringField("value", Base64.getEncoder().encodeToString(bytes));
        } else {
            jgen.writeStringField("type", String.class.getSimpleName());
            jgen.writeStringField("value", source.getValue().toString());
        }

        jgen.writeEndObject();
    }

}
