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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.apache.syncope.common.lib.AMSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMSessionDeserializer extends StdDeserializer<AMSession> {

    private static final long serialVersionUID = 24527200564172L;

    private static final Logger LOG = LoggerFactory.getLogger(AMSessionDeserializer.class);

    public AMSessionDeserializer() {
        this(null);
    }

    public AMSessionDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public AMSession deserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        AMSession waSession = new AMSession();

        if (node.has("authentication_date_formatted")) {
            String authenticationDate = node.get("authentication_date_formatted").textValue();
            try {
                waSession.setAuthenticationDate(
                        OffsetDateTime.parse(authenticationDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } catch (DateTimeParseException e) {
                LOG.error("Unparsable date: {}", authenticationDate, e);
            }
        }

        if (node.has("authenticated_principal")) {
            waSession.setPrincipal(node.get("authenticated_principal").textValue());
        }

        if (node.has("ticket_granting_ticket")) {
            waSession.setKey(node.get("ticket_granting_ticket").textValue());
        }

        StringWriter writer = new StringWriter();
        JsonGenerator jgen = jp.getCodec().getFactory().createGenerator(writer);
        jgen.setPrettyPrinter(new DefaultPrettyPrinter());
        jp.getCodec().writeTree(jgen, node);
        waSession.setJson(writer.toString());

        return waSession;
    }
}
