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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;

@SuppressWarnings("squid:S3776")
class AttributeDeltaDeserializer extends AbstractValueDeserializer<AttributeDelta> {

    @Override
    public AttributeDelta deserialize(final JsonParser jp, final DeserializationContext ctx) throws IOException {
        ObjectNode tree = jp.readValueAsTree();

        String name = tree.get("name").asText();

        List<Object> valuesToAdd = doDeserialize(tree.get("valuesToAdd"), jp);
        List<Object> valuesToRemove = doDeserialize(tree.get("valuesToRemove"), jp);

        JsonNode valuesToReplaceNode = tree.get("valuesToReplace");
        List<Object> valuesToReplace = doDeserialize(valuesToReplaceNode, jp);

        return valuesToReplaceNode.isNull()
                ? AttributeDeltaBuilder.build(name, valuesToAdd, valuesToRemove)
                : AttributeDeltaBuilder.build(name, valuesToReplace);
    }
}
