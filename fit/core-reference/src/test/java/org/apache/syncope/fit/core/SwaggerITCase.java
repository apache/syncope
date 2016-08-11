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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Assume;
import org.junit.Test;

public class SwaggerITCase extends AbstractITCase {

    @Test
    public void swagger() throws IOException {
        WebClient webClient = WebClient.create(ADDRESS + "/swagger.json").accept(MediaType.APPLICATION_JSON_TYPE);
        Response response = webClient.get();
        Assume.assumeTrue(response.getStatus() == 200);

        JsonNode tree = new ObjectMapper().readTree((InputStream) response.getEntity());
        assertNotNull(tree);

        JsonNode info = tree.get("info");
        assertEquals("Apache Syncope", info.get("title").asText());

        assertEquals("/syncope/rest", tree.get("basePath").asText());

        JsonNode tags = tree.get("tags");
        assertNotNull(tags);
        assertTrue(tags.isContainerNode());

        JsonNode paths = tree.get("paths");
        assertNotNull(paths);
        assertTrue(paths.isContainerNode());

        JsonNode definitions = tree.get("definitions");
        assertNotNull(definitions);
        assertTrue(definitions.isContainerNode());
    }
}
