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
package org.apache.syncope.common.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.junit.jupiter.api.Test;

class SyncopeClientExceptionTest {

    @Test
    void build() {
        SyncopeClientException exception = SyncopeClientException.build(ClientExceptionType.InvalidUser);

        assertEquals(ClientExceptionType.InvalidUser, exception.getType());
        assertFalse(exception.isComposite());
        assertTrue(exception.isEmpty());
        assertEquals(0, exception.size());
        assertEquals("InvalidUser []", exception.getMessage());
        assertEquals(exception.getMessage(), exception.getLocalizedMessage());

        exception.getElements().add("element");
        assertFalse(exception.isEmpty());
        assertEquals(1, exception.size());
        assertEquals("InvalidUser [element]", exception.getMessage());
    }

    @Test
    void buildCompositeType() {
        assertThrows(IllegalArgumentException.class,
                () -> SyncopeClientException.build(ClientExceptionType.Composite));
    }

    @Test
    void asComposite() {
        SyncopeClientCompositeException composite = SyncopeClientException.buildComposite();

        assertTrue(composite.isComposite());
        assertSame(composite, composite.asComposite());

        SyncopeClientException exception = SyncopeClientException.build(ClientExceptionType.NotFound);
        assertThrows(IllegalArgumentException.class, exception::asComposite);
    }

    @Test
    void addException() {
        SyncopeClientCompositeException composite = SyncopeClientException.buildComposite();
        assertFalse(composite.hasExceptions());

        SyncopeClientException first = SyncopeClientException.build(ClientExceptionType.InvalidUser);
        first.getElements().add("one");
        assertTrue(composite.addException(first));

        assertTrue(composite.hasExceptions());
        assertEquals(1, composite.getExceptions().size());
        assertTrue(composite.getException(ClientExceptionType.InvalidUser).isPresent());
        assertTrue(composite.getException(ClientExceptionType.NotFound).isEmpty());
        assertEquals("{[InvalidUser [one]]}", composite.getMessage());
        assertEquals(composite.getMessage(), composite.getLocalizedMessage());

        SyncopeClientException second = SyncopeClientException.build(ClientExceptionType.InvalidUser);
        second.getElements().add("two");
        composite.addException(second);

        assertEquals(1, composite.getExceptions().size());
        assertEquals(2, composite.getException(ClientExceptionType.InvalidUser).orElseThrow().getElements().size());
    }

    @Test
    void addExceptionWithNullType() {
        SyncopeClientCompositeException composite = SyncopeClientException.buildComposite();

        SyncopeClientException exception = SyncopeClientException.build(null);
        composite.addException(exception);

        assertEquals(ClientExceptionType.Unknown, exception.getType());
        assertTrue(composite.getException(ClientExceptionType.Unknown).isPresent());
    }
}
