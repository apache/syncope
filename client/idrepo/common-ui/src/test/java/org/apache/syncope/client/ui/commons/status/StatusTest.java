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
package org.apache.syncope.client.ui.commons.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatusTest {

    @Test
    void values() {
        assertEquals(6, Status.values().length);

        assertEquals("", Status.NOT_YET_SUBMITTED.toString());
        assertEquals("created", Status.CREATED.toString());
        assertEquals("active", Status.ACTIVE.toString());
        assertEquals("inactive", Status.SUSPENDED.toString());
        assertEquals("undefined", Status.UNDEFINED.toString());
        assertEquals("objectnotfound", Status.OBJECT_NOT_FOUND.toString());
    }

    @Test
    void isActive() {
        assertTrue(Status.ACTIVE.isActive());
        assertFalse(Status.NOT_YET_SUBMITTED.isActive());
        assertFalse(Status.CREATED.isActive());
        assertFalse(Status.SUSPENDED.isActive());
        assertFalse(Status.UNDEFINED.isActive());
        assertFalse(Status.OBJECT_NOT_FOUND.isActive());
    }

    @Test
    void valueOf() {
        assertEquals(Status.ACTIVE, Status.valueOf("ACTIVE"));
        assertEquals(Status.SUSPENDED, Status.valueOf("SUSPENDED"));
        assertEquals(Status.NOT_YET_SUBMITTED, Status.valueOf("NOT_YET_SUBMITTED"));
    }
}
