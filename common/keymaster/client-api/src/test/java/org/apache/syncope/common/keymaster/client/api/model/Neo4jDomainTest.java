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
package org.apache.syncope.common.keymaster.client.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Test;

class Neo4jDomainTest {

    @Test
    void domain() {
        Neo4jDomain domain = new Neo4jDomain.Builder("key").
                uri(URI.create("neo4j://localhost:7687")).
                build();

        assertEquals("key", domain.getKey());
        assertEquals("neo4j://localhost:7687", domain.getUri().toString());

        Neo4jDomain same = new Neo4jDomain.Builder("key").
                uri(URI.create("neo4j://localhost:7687")).
                build();

        Neo4jDomain other = new Neo4jDomain.Builder("other").
                uri(URI.create("neo4j://localhost:7687")).
                build();

        assertEquals(domain, domain);
        assertEquals(domain, same);
        assertEquals(domain.hashCode(), same.hashCode());
        assertTrue(!domain.equals(other));
    }
}
