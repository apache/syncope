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
package org.apache.syncope.common.lib.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.junit.jupiter.api.Test;

class SearchableFieldsTest {

    @Test
    void contains() {
        assertTrue(SearchableFields.contains("username"));
        assertTrue(SearchableFields.contains("name"));
        assertTrue(SearchableFields.contains("key"));
        assertFalse(SearchableFields.contains("nonExistent"));
        assertFalse(SearchableFields.contains("password"));
    }

    @Test
    void user() {
        Map<String, AttrSchemaType> fields = SearchableFields.get(UserTO.class);

        assertEquals(AttrSchemaType.String, fields.get("username"));
        assertEquals(AttrSchemaType.Date, fields.get("lastLoginDate"));
        assertEquals(AttrSchemaType.Boolean, fields.get("suspended"));
        assertEquals(AttrSchemaType.Boolean, fields.get("mustChangePassword"));
        assertEquals(AttrSchemaType.Date, fields.get("creationDate"));
        assertEquals(AttrSchemaType.String, fields.get("realm"));

        assertFalse(fields.containsKey("password"));
        assertFalse(fields.containsKey("type"));
        assertFalse(fields.containsKey("discriminator"));
        assertFalse(fields.containsKey("memberships"));
        assertFalse(fields.containsKey("roles"));
        assertFalse(fields.containsKey("linkedAccounts"));
    }

    @Test
    void group() {
        Map<String, AttrSchemaType> fields = SearchableFields.get(GroupTO.class);

        assertEquals(AttrSchemaType.String, fields.get("name"));
        assertEquals(AttrSchemaType.Long, fields.get("userMembershipCount"));
        assertEquals(AttrSchemaType.Long, fields.get("anyObjectMembershipCount"));
        assertFalse(fields.containsKey("typeExtensions"));
    }

    @Test
    void anyObject() {
        Map<String, AttrSchemaType> fields = SearchableFields.get(AnyObjectTO.class);

        assertEquals(AttrSchemaType.String, fields.get("name"));
        assertFalse(fields.containsKey("memberships"));
    }
}
