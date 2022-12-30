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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class OpenAPIITCase extends AbstractITCase {

    @Test
    public void openapi() throws IOException {
        WebClient webClient = WebClient.create(ADDRESS + "/openapi.json").accept(MediaType.APPLICATION_JSON_TYPE);
        Response response = webClient.get();
        assertEquals(200, response.getStatus());

        JsonNode tree = JSON_MAPPER.readTree((InputStream) response.getEntity());
        assertNotNull(tree);

        JsonNode info = tree.get("info");
        assertEquals("Apache Syncope", info.get("title").asText());

        JsonNode paths = tree.get("paths");
        assertNotNull(paths);
        assertTrue(paths.isContainerNode());

        JsonNode components = tree.get("components");
        assertNotNull(components);
        assertTrue(components.isContainerNode());
    }
}
