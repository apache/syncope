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

import java.util.Base64;
import org.identityconnectors.framework.common.objects.SyncToken;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

class SyncTokenSerializer extends ValueSerializer<SyncToken> {

    @Override
    public void serialize(final SyncToken source, final JsonGenerator jgen, final SerializationContext ctx)
            throws JacksonException {

        jgen.writeStartObject();

        if (source.getValue() == null) {
            jgen.writeNullProperty("value");
        } else if (source.getValue() instanceof final Boolean b) {
            jgen.writeStringProperty("type", Boolean.class.getSimpleName());
            jgen.writeBooleanProperty("value", b);
        } else if (source.getValue() instanceof final Double v) {
            jgen.writeStringProperty("type", Double.class.getSimpleName());
            jgen.writeNumberProperty("value", v);
        } else if (source.getValue() instanceof final Long l) {
            jgen.writeStringProperty("type", Long.class.getSimpleName());
            jgen.writeNumberProperty("value", l);
        } else if (source.getValue() instanceof final Integer i) {
            jgen.writeStringProperty("type", Integer.class.getSimpleName());
            jgen.writeNumberProperty("value", i);
        } else if (source.getValue() instanceof final byte[] bytes) {
            jgen.writeStringProperty("value", Base64.getEncoder().encodeToString(bytes));
        } else {
            jgen.writeStringProperty("type", String.class.getSimpleName());
            jgen.writeStringProperty("value", source.getValue().toString());
        }

        jgen.writeEndObject();
    }
}
