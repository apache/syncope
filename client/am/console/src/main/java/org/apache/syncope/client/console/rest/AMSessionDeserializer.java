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
package org.apache.syncope.client.console.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.apache.syncope.common.lib.AMSession;
import org.apache.syncope.common.lib.jackson.SyncopeJsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;

public class AMSessionDeserializer extends StdDeserializer<AMSession> {

    protected static final Logger LOG = LoggerFactory.getLogger(AMSessionDeserializer.class);

    protected static final JsonMapper MAPPER = new SyncopeJsonMapper();

    public AMSessionDeserializer() {
        this(null);
    }

    public AMSessionDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public AMSession deserialize(final JsonParser jp, final DeserializationContext ctxt) throws JacksonException {
        JsonNode node = jp.readValueAsTree();

        AMSession waSession = new AMSession();

        if (node.has("authentication_date_formatted")) {
            String authenticationDate = node.get("authentication_date_formatted").stringValue();
            try {
                waSession.setAuthenticationDate(
                        OffsetDateTime.parse(authenticationDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } catch (DateTimeParseException e) {
                LOG.error("Unparsable date: {}", authenticationDate, e);
            }
        }

        if (node.has("authenticated_principal")) {
            waSession.setPrincipal(node.get("authenticated_principal").stringValue());
        }

        if (node.has("ticket_granting_ticket")) {
            waSession.setKey(node.get("ticket_granting_ticket").stringValue());
        }

        try (StringWriter writer = new StringWriter()) {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, node);
            waSession.setJson(writer.toString());
        } catch (IOException e) {
            throw JacksonIOException.construct(e);
        }

        return waSession;
    }
}
