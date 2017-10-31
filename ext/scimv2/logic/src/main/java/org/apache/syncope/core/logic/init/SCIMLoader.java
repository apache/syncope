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
package org.apache.syncope.core.logic.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SCIMLoader implements SyncopeLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SCIMLoader.class);

    private static final String SCIMV2_LOGIC_PROPERTIES = "scimv2-logic.properties";

    private static final String SCHEMAS = "schemas.json";

    private int bulkMaxOperations = 0;

    private int bulkMaxPayloadSize = 0;

    private int filterMaxResults = 0;

    private String schemas;

    private final Map<String, String> schemaMap = new HashMap<>();

    @Override
    public Integer getPriority() {
        return 1000;
    }

    @Override
    public void load() {
        Pair<Properties, String> init = PropertyUtils.read(getClass(), SCIMV2_LOGIC_PROPERTIES, "conf.directory");
        Properties props = init.getLeft();

        bulkMaxOperations = Integer.valueOf(props.getProperty("bulk.maxOperations"));
        bulkMaxPayloadSize = Integer.valueOf(props.getProperty("bulk.maxPayloadSize"));
        filterMaxResults = Integer.valueOf(props.getProperty("filter.maxResults"));

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode tree = mapper.readTree(getClass().getResourceAsStream("/" + SCHEMAS));
            if (!tree.isArray()) {
                throw new IOException("JSON node is not a tree");
            }

            ArrayNode schemaArray = (ArrayNode) tree;
            schemas = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);

            for (JsonNode schema : schemaArray) {
                schemaMap.put(schema.get("id").asText(), mapper.writeValueAsString(schema));
            }
        } catch (IOException e) {
            LOG.error("Could not parse the default schema definitions", e);
        }
    }

    public int getBulkMaxOperations() {
        return bulkMaxOperations;
    }

    public int getBulkMaxPayloadSize() {
        return bulkMaxPayloadSize;
    }

    public int getFilterMaxResults() {
        return filterMaxResults;
    }

    public String getSchemas() {
        return schemas;
    }

    public String getSchema(final String schema) {
        return schemaMap.get(schema);
    }

}
