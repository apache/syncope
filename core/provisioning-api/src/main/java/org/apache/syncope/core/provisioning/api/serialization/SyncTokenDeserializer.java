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
    public SyncToken deserialize(final JsonParser jp, final DeserializationContext ctx) throws IOException {
        ObjectNode tree = jp.readValueAsTree();

        Object value = tree.has("value")
                ? tree.has("type")
                ? deserialize(tree.get("value"), tree.get("type"))
                : deserialize(tree.get("value"))
                : null;

        return new SyncToken(Objects.requireNonNull(value));
    }

    private Object deserialize(final JsonNode value, final JsonNode type) {
        if (Boolean.class.getSimpleName().equals(type.asText())) {
            return value.asBoolean();
        }

        if (Double.class.getSimpleName().equals(type.asText())) {
            return value.asDouble();
        }
        if (Long.class.getSimpleName().equals(type.asText())) {
            return value.asLong();
        }
        if (Integer.class.getSimpleName().equals(type.asText())) {
            return value.asInt();
        }

        return value.asText();
    }

    private Object deserialize(final JsonNode value) {
        if (value.isBoolean()) {
            return value.asBoolean();
        }

        if (value.isDouble()) {
            return value.asDouble();
        }

        if (value.isLong()) {
            return value.asLong();
        }

        if (value.isInt()) {
            return value.asInt();
        }

        try {
            return Base64.getDecoder().decode(value.asText());
        } catch (RuntimeException e) {
            return value.asText();
        }
    }
}
