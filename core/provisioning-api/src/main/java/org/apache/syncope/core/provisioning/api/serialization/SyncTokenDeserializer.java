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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

import org.identityconnectors.framework.common.objects.SyncToken;

class SyncTokenDeserializer extends JsonDeserializer<SyncToken> {

    @Override
    public SyncToken deserialize(final JsonParser jp, final DeserializationContext ctx)
            throws IOException {

        ObjectNode tree = jp.readValueAsTree();

        Object value = null;
        if (tree.has("value")) {
            JsonNode node = tree.get("value");
            if (node.isBoolean()) {
                value = node.asBoolean();
            } else if (node.isDouble()) {
                value = node.asDouble();
            } else if (node.isLong()) {
                value = node.asLong();
            } else if (node.isInt()) {
                value = node.asInt();
            } else {
                value = node.asText();
            }

            if (value instanceof String) {
                String base64 = (String) value;
                try {
                    value = Base64.getDecoder().decode(base64);
                } catch (RuntimeException e) {
                    value = base64;
                }
            }
        }

        return new SyncToken(Objects.requireNonNull(value));
    }

}
