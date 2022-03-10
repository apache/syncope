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

import java.time.OffsetDateTime;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.jupiter.api.Test;

public class AttrSchemaTypeTest {

    @Test
    void checkLong() {
        assertEquals(AttrSchemaType.Long, AttrSchemaType.getAttrSchemaTypeByClass(Long.class));
        assertEquals(AttrSchemaType.Long, AttrSchemaType.getAttrSchemaTypeByClass(long.class));
    }

    @Test
    void checkDouble() {
        assertEquals(AttrSchemaType.Double, AttrSchemaType.getAttrSchemaTypeByClass(Double.class));
        assertEquals(AttrSchemaType.Double, AttrSchemaType.getAttrSchemaTypeByClass(double.class));
    }

    @Test
    void checkBoolean() {
        assertEquals(AttrSchemaType.Boolean, AttrSchemaType.getAttrSchemaTypeByClass(Boolean.class));
        assertEquals(AttrSchemaType.Boolean, AttrSchemaType.getAttrSchemaTypeByClass(boolean.class));
    }

    @Test
    void checkDate() {
        assertEquals(AttrSchemaType.Date, AttrSchemaType.getAttrSchemaTypeByClass(OffsetDateTime.class));
    }

    @Test
    void checkEnum() {
        assertEquals(AttrSchemaType.Enum, AttrSchemaType.getAttrSchemaTypeByClass(Enum.class));
        assertEquals(AttrSchemaType.Enum, AttrSchemaType.getAttrSchemaTypeByClass(CipherAlgorithm.class));
    }

    @Test
    void checkBinary() {
        assertEquals(AttrSchemaType.Binary, AttrSchemaType.getAttrSchemaTypeByClass(Byte[].class));
        assertEquals(AttrSchemaType.Binary, AttrSchemaType.getAttrSchemaTypeByClass(byte[].class));
    }

    @Test
    void checkString() {
        assertEquals(AttrSchemaType.String, AttrSchemaType.getAttrSchemaTypeByClass(String.class));
        assertEquals(AttrSchemaType.String, AttrSchemaType.getAttrSchemaTypeByClass(UserTO.class));
    }
}
