/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the License.  You may not use this file except in compliance
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
package org.apache.syncope.core.persistence.api.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AllowedSchemasTest {

    @Mock
    private Schema schema;

    @Mock
    private Group group;

    @Mock
    private RelationshipType relationshipType;

    @Test
    void selfContains() {
        AllowedSchemas<Schema> allowedSchemas = new AllowedSchemas<>();
        final Schema other = Mockito.mock(Schema.class);

        assertFalse(allowedSchemas.selfContains(schema));
        allowedSchemas.self().add(schema);
        assertTrue(allowedSchemas.selfContains(schema));
        assertFalse(allowedSchemas.selfContains(other));
    }

    @Test
    void selfContainsByName() {
        AllowedSchemas<Schema> allowedSchemas = new AllowedSchemas<>();
        final Schema other = Mockito.mock(Schema.class);

        allowedSchemas.self().add(schema);
        Mockito.when(schema.getKey()).thenReturn("this-schema");

        assertTrue(allowedSchemas.selfContains("this-schema"));
        assertFalse(allowedSchemas.selfContains("other-schema"));
        assertFalse(allowedSchemas.selfContains(other));
    }

    @Test
    void membershipsContains() {
        AllowedSchemas<Schema> allowedSchemas = new AllowedSchemas<>();
        final Set<Schema> schemas = new HashSet<>();
        schemas.add(schema);

        assertFalse(allowedSchemas.membershipsContains(group, schema));
        allowedSchemas.memberships().put(group, schemas);
        assertTrue(allowedSchemas.membershipsContains(group, schema));
    }

    @Test
    void membershipsContainsByName() {
        AllowedSchemas<Schema> allowedSchemas = new AllowedSchemas<>();
        final Set<Schema> schemas = new HashSet<>();
        schemas.add(schema);

        allowedSchemas.memberships().put(group, schemas);
        Mockito.when(schema.getKey()).thenReturn("this-schema");

        assertTrue(allowedSchemas.membershipsContains(group, "this-schema"));
        assertFalse(allowedSchemas.membershipsContains(group, "missing-schema"));
    }

    @Test
    void relationshipTypesContains() {
        AllowedSchemas<Schema> allowedSchemas = new AllowedSchemas<>();
        final Set<Schema> schemas = new HashSet<>();
        schemas.add(schema);

        assertFalse(allowedSchemas.relationshipTypesContains(relationshipType, schema));
        allowedSchemas.relationshipTypes().put(relationshipType, schemas);
        assertTrue(allowedSchemas.relationshipTypesContains(relationshipType, schema));
    }

    @Test
    void relationshipTypesContainsByName() {
        AllowedSchemas<Schema> allowedSchemas = new AllowedSchemas<>();
        final Set<Schema> schemas = new HashSet<>();
        schemas.add(schema);

        allowedSchemas.relationshipTypes().put(relationshipType, schemas);
        Mockito.when(schema.getKey()).thenReturn("this-schema");

        assertTrue(allowedSchemas.relationshipTypesContains(relationshipType, "this-schema"));
        assertFalse(allowedSchemas.relationshipTypesContains(relationshipType, "missing-schema"));
    }
}
