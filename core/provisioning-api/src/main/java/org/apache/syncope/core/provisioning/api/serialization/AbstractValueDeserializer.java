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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.security.GuardedString;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

public abstract class AbstractValueDeserializer<T extends Object> extends ValueDeserializer<T> {

    protected List<Object> doDeserialize(final JsonNode value, final JsonParser jp) throws JacksonException {
        List<Object> values = new ArrayList<>();

        for (JsonNode node : value) {
            if (node.isNull()) {
                values.add(null);
            } else if (node.isObject()) {
                values.add(node.traverse(jp.objectReadContext()).readValueAs(GuardedString.class));
            } else if (node.isBoolean()) {
                values.add(node.asBoolean());
            } else if (node.isDouble()) {
                values.add(node.asDouble());
            } else if (node.isLong()) {
                values.add(node.asLong());
            } else if (node.isInt()) {
                values.add(node.asInt());
            } else {
                String text = node.asString();
                if (text.startsWith(AbstractValueSerializer.BYTE_ARRAY_PREFIX)
                        && text.endsWith(AbstractValueSerializer.BYTE_ARRAY_SUFFIX)) {

                    values.add(Base64.getDecoder().decode(StringUtils.substringBetween(
                            text,
                            AbstractValueSerializer.BYTE_ARRAY_PREFIX,
                            AbstractValueSerializer.BYTE_ARRAY_SUFFIX)));
                } else {
                    values.add(text);
                }
            }
        }

        return values;
    }
}
