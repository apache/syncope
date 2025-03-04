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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;

@SuppressWarnings("squid:S3776")
class AttributeDeserializer extends AbstractValueDeserializer<Attribute> {

    @Override
    public Attribute deserialize(final JsonParser jp, final DeserializationContext ctx) throws IOException {
        ObjectNode tree = jp.readValueAsTree();

        String name = tree.get("name").asText();

        List<Object> values = doDeserialize(tree.get("value"), jp);

        if (Uid.NAME.equals(name)) {
            return new Uid(values.isEmpty() || values.getFirst() == null ? null : values.getFirst().toString());
        } else {
            if (Name.NAME.equals(name)) {
                return new Name(values.isEmpty() || values.getFirst() == null ? null : values.getFirst().toString());
            }

            return AttributeBuilder.build(name, values);
        }
    }
}
