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
package org.apache.syncope.ext.scimv2.api.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;
import org.junit.jupiter.api.Test;

class SCIMSearchRequestTest {

    @Test
    void defaults() {
        SCIMSearchRequest request = new SCIMSearchRequest(null, null, null, null, null);

        assertEquals(1, request.getStartIndex());
        assertEquals(25, request.getCount());
        assertNull(request.getFilter());
        assertNull(request.getSortBy());
        assertNull(request.getSortOrder());
        assertTrue(request.getAttributes().isEmpty());
        assertTrue(request.getExcludedAttributes().isEmpty());
        assertEquals(List.of(Resource.SearchRequest.schema()), request.getSchemas());
    }

    @Test
    void explicitValues() {
        SCIMSearchRequest request = new SCIMSearchRequest("userName eq \"bjensen\"", "userName",
                SortOrder.descending, 11, 10);

        assertEquals("userName eq \"bjensen\"", request.getFilter());
        assertEquals("userName", request.getSortBy());
        assertEquals(SortOrder.descending, request.getSortOrder());
        assertEquals(11, request.getStartIndex());
        assertEquals(10, request.getCount());
        assertEquals(List.of(Resource.SearchRequest.schema()), request.getSchemas());
    }
}
