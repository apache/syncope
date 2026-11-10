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
package org.apache.syncope.common.lib.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EntityViolationTypeTest {

    @AfterEach
    void cleanUp() {
        EntityViolationType.InvalidUsername.message(null).propertyPath(null).invalidValue(null);
    }

    @Test
    void defaults() {
        assertNull(EntityViolationType.InvalidUsername.getMessage());
        assertNull(EntityViolationType.InvalidUsername.getPropertyPath());
        assertNull(EntityViolationType.InvalidUsername.getInvalidValue());
    }

    @Test
    void builders() {
        EntityViolationType type = EntityViolationType.InvalidUsername.
                message("error").
                propertyPath("username").
                invalidValue("admin");

        assertSame(EntityViolationType.InvalidUsername, type);
        assertEquals("error", type.getMessage());
        assertEquals("username", type.getPropertyPath());
        assertEquals("admin", type.getInvalidValue());
        assertEquals("InvalidUsername{message=error, propertyPath=username, invalidValue=admin}", type.toString());
    }
}
