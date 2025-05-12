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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectIdentification;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

public class ConnectorObjectIdentificationDeserializer
        extends AbstractValueDeserializer<ConnectorObjectIdentification> {
    @Override
    public ConnectorObjectIdentification deserialize(final JsonParser jp,
            final DeserializationContext deserializationContext) throws IOException {

        ObjectNode tree = jp.readValueAsTree();

        String objectClass = tree.get("objectClass").asText();

        Set<Attribute> attributes = new HashSet<>();
        JsonNode attributesNode = tree.get("attributes");
        if (attributesNode != null && attributesNode.isArray()) {
            attributesNode.forEach(attrNode -> {
                try {
                    String name = attrNode.get("name").asText();
                    List<Object> values = doDeserialize(attrNode.get("value"), jp);
                    attributes.add(Uid.NAME.equals(name) 
                            ? new Uid(values.isEmpty() || values.getFirst() == null 
                            ? null : values.getFirst().toString())
                            : Name.NAME.equals(name)
                                    ? new Name(values.isEmpty() || values.getFirst() == null
                                    ? null : values.getFirst().toString())
                                    : AttributeBuilder.build(name, values));
                } catch (IOException e) {
                }
            });
        }

        return new ConnectorObjectIdentification(new ObjectClass(objectClass), attributes);
    }
}
