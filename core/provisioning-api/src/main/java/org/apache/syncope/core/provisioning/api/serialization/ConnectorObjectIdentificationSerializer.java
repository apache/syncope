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

import org.identityconnectors.framework.common.objects.ConnectorObjectIdentification;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

public class ConnectorObjectIdentificationSerializer extends AbstractValueSerializer<ConnectorObjectIdentification> {

    @Override
    public void serialize(
            final ConnectorObjectIdentification value,
            final JsonGenerator jgen,
            final SerializationContext ctx) throws JacksonException {

        jgen.writeStartObject();

        jgen.writeStringProperty("objectClass", value.getObjectClass().getObjectClassValue());

        jgen.writeName("attributes");

        jgen.writeStartArray();
        value.getAttributes().forEach(attr -> {
            jgen.writeStartObject();

            jgen.writeStringProperty("name", attr.getName());

            jgen.writeName("value");
            doSerialize(attr.getValue(), jgen);
            jgen.writeEndObject();
        });
        jgen.writeEndArray();

        jgen.writeEndObject();
    }
}
