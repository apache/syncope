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
package org.apache.syncope.core.persistence.neo4j.entity.keymaster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import org.apache.syncope.core.persistence.api.entity.keymaster.ConfParam;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractProvidedKeyNode;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jConfParam.NODE)
public class Neo4jConfParam extends AbstractProvidedKeyNode implements ConfParam {

    private static final long serialVersionUID = 8742750097008236475L;

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    public static final String NODE = "ConfParam";

    private String jsonValue;

    @Override
    public JsonNode getValue() {
        JsonNode deserialized = null;
        try {
            deserialized = MAPPER.readTree(jsonValue);
        } catch (final IOException e) {
            LOG.error("Could not deserialize {}", jsonValue, e);
        }
        return deserialized;
    }

    @Override
    public void setValue(final JsonNode value) {
        try {
            this.jsonValue = MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            LOG.error("Could not serialize {}", value, e);
        }
    }
}
