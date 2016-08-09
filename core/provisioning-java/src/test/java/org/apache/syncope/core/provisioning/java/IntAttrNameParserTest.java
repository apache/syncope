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
package org.apache.syncope.core.provisioning.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class IntAttrNameParserTest extends AbstractTest {

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Test
    public void ownFields() {
        IntAttrName intAttrName = intAttrNameParser.parse("key", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNotNull(intAttrName.getField());
        assertEquals("key", intAttrName.getField());
        assertNull(intAttrName.getSchemaName());
        assertNull(intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());

        intAttrName = intAttrNameParser.parse("name", AnyTypeKind.GROUP);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.GROUP, intAttrName.getAnyTypeKind());
        assertNotNull(intAttrName.getField());
        assertEquals("name", intAttrName.getField());
        assertNull(intAttrName.getSchemaName());
        assertNull(intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());

        intAttrName = intAttrNameParser.parse("userOwner", AnyTypeKind.GROUP);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.GROUP, intAttrName.getAnyTypeKind());
        assertNotNull(intAttrName.getField());
        assertEquals("userOwner", intAttrName.getField());
        assertNull(intAttrName.getSchemaName());
        assertNull(intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());

        intAttrName = intAttrNameParser.parse("name", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
    }

    @Test
    public void ownSchema() {
        IntAttrName intAttrName = intAttrNameParser.parse("email", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("email", intAttrName.getSchemaName());
        assertEquals(SchemaType.PLAIN, intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());

        intAttrName = intAttrNameParser.parse("cn", AnyTypeKind.ANY_OBJECT);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.ANY_OBJECT, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("cn", intAttrName.getSchemaName());
        assertEquals(SchemaType.DERIVED, intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());

        intAttrName = intAttrNameParser.parse("rvirtualdata", AnyTypeKind.ANY_OBJECT);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.ANY_OBJECT, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("rvirtualdata", intAttrName.getSchemaName());
        assertEquals(SchemaType.VIRTUAL, intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
    }

    @Test
    public void enclosingGroup() {
        IntAttrName intAttrName = intAttrNameParser.parse("groups[readers].cn", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.GROUP, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("cn", intAttrName.getSchemaName());
        assertEquals(SchemaType.DERIVED, intAttrName.getSchemaType());
        assertEquals("readers", intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
    }

    @Test
    public void relatedAnyObject() {
        IntAttrName intAttrName = intAttrNameParser.parse("anyObjects[hp].name", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.ANY_OBJECT, intAttrName.getAnyTypeKind());
        assertEquals("name", intAttrName.getField());
        assertNull(intAttrName.getSchemaName());
        assertNull(intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertEquals("hp", intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getMembershipOfGroup());
    }

    @Test
    public void membership() {
        IntAttrName intAttrName = intAttrNameParser.parse("memberships[top].cn", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("cn", intAttrName.getSchemaName());
        assertEquals(SchemaType.DERIVED, intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertEquals("top", intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
    }

    @Test
    public void invalid() {
        try {
            intAttrNameParser.parse("memberships.cn", AnyTypeKind.USER);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }
}
