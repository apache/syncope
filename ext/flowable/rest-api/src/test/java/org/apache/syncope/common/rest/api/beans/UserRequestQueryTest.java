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
package org.apache.syncope.common.rest.api.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class UserRequestQueryTest {

    @Test
    void builder() {
        UserRequestQuery query = new UserRequestQuery.Builder().
                user("userKey").
                page(2).
                size(50).
                orderBy("bpmnProcess DESC").
                build();

        assertEquals("userKey", query.getUser());
        assertEquals(2, query.getPage());
        assertEquals(50, query.getSize());
        assertEquals("bpmnProcess DESC", query.getOrderBy());
    }

    @Test
    void defaultPaging() {
        UserRequestQuery query = new UserRequestQuery.Builder().page(0).size(0).build();

        assertEquals(1, query.getPage());
        assertEquals(1, query.getSize());
        assertNull(query.getOrderBy());
    }
}
