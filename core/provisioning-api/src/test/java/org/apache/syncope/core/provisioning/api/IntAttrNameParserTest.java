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
package org.apache.syncope.core.provisioning.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class IntAttrNameParserTest extends AbstractTest {

    private static final Map<AnyTypeKind, List<String>> FIELDS = new HashMap<>();

    static {
        FIELDS.put(AnyTypeKind.USER, List.of("key", "username"));
        FIELDS.put(AnyTypeKind.GROUP, List.of("key", "name", "userOwner"));
        FIELDS.put(AnyTypeKind.ANY_OBJECT, List.of("key", "name"));
    }

    @Mock
    private PlainSchemaDAO plainSchemaDAO;

    @Mock
    private DerSchemaDAO derSchemaDAO;

    @Mock
    private VirSchemaDAO virSchemaDAO;

    @Mock
    private AnyUtilsFactory anyUtilsFactory;

    @Mock
    private AnyUtils anyUtils;

    private IntAttrNameParser intAttrNameParser;

    @BeforeEach
    public void initMocks() throws NoSuchFieldException {
        lenient().when(anyUtilsFactory.getInstance(any(AnyTypeKind.class))).thenAnswer(ic -> {
            when(anyUtils.anyTypeKind()).thenReturn(ic.getArgument(0));
            return anyUtils;
        });
        lenient().when(anyUtils.getField(anyString())).thenAnswer(ic -> {
            String fieldName = ic.getArgument(0);
            if (FIELDS.get(anyUtils.anyTypeKind()).contains(fieldName)) {
                Field field = mock(Field.class);
                when(field.getName()).thenReturn(fieldName);
                when(field.getType()).thenAnswer(ic2 -> String.class);
                return Optional.of(field);
            }
            return Optional.empty();
        });
        lenient().when(plainSchemaDAO.findById(anyString())).thenAnswer(ic -> {
            String schemaName = ic.getArgument(0);
            switch (schemaName) {
                case "email", "firstname", "location" -> {
                    PlainSchema schema = mock(PlainSchema.class);
                    lenient().when(schema.getKey()).thenReturn(schemaName);
                    lenient().when(schema.getType()).thenReturn(AttrSchemaType.String);
                    return Optional.of(schema);
                }
                default -> {
                    return Optional.empty();
                }
            }
        });
        lenient().when(derSchemaDAO.findById(anyString())).thenAnswer(ic -> {
            String schemaName = ic.getArgument(0);
            switch (schemaName) {
                case "cn" -> {
                    DerSchema schema = mock(DerSchema.class);
                    lenient().when(schema.getKey()).thenReturn(ic.getArgument(0));
                    return Optional.of(schema);
                }

                default -> {
                    return Optional.empty();
                }
            }
        });
        lenient().when(virSchemaDAO.findById(anyString())).thenAnswer(ic -> {
            String schemaName = ic.getArgument(0);
            switch (schemaName) {
                case "rvirtualdata" -> {
                    VirSchema schema = mock(VirSchema.class);
                    lenient().when(schema.getKey()).thenReturn(ic.getArgument(0));
                    return Optional.of(schema);
                }

                default -> {
                    return Optional.empty();
                }
            }
        });

        intAttrNameParser = new IntAttrNameParser(plainSchemaDAO, derSchemaDAO, virSchemaDAO, anyUtilsFactory);
    }

    @Test
    public void ownFields() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("key", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNotNull(intAttrName.getField());
        assertEquals("key", intAttrName.getField());
        assertNull(intAttrName.getSchema());
        assertNull(intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());

        intAttrName = intAttrNameParser.parse("name", AnyTypeKind.GROUP);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.GROUP, intAttrName.getAnyTypeKind());
        assertNotNull(intAttrName.getField());
        assertEquals("name", intAttrName.getField());
        assertNull(intAttrName.getSchema());
        assertNull(intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());

        intAttrName = intAttrNameParser.parse("userOwner", AnyTypeKind.GROUP);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.GROUP, intAttrName.getAnyTypeKind());
        assertNotNull(intAttrName.getField());
        assertEquals("userOwner", intAttrName.getField());
        assertNull(intAttrName.getSchema());
        assertNull(intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());

        intAttrName = intAttrNameParser.parse("name", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());

        Object nullObj = null;
        int expected = new HashCodeBuilder().
                append(AnyTypeKind.USER).append(nullObj).append(nullObj).append(nullObj).append(nullObj).
                append(nullObj).append(nullObj).append(nullObj).append(nullObj).append(nullObj).
                build();
        assertEquals(expected, intAttrName.hashCode());
        IntAttrName intAttrName2 = intAttrNameParser.parse("email", AnyTypeKind.USER);
        assertFalse(intAttrName.equals(intAttrName2));
        assertFalse(intAttrName.equals(nullObj));
        assertTrue(intAttrName.equals(intAttrName));
        String toString = intAttrName.toString();
        assertTrue(toString.startsWith("org.apache.syncope.core.provisioning.api.IntAttrName"));
        assertTrue(toString.endsWith("[USER,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>]"));
    }

    @Test
    public void ownSchema() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("email", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("email", intAttrName.getSchema().getKey());
        assertEquals(SchemaType.PLAIN, intAttrName.getSchemaType());
        assertTrue(intAttrName.getSchema() instanceof PlainSchema);
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());

        intAttrName = intAttrNameParser.parse("cn", AnyTypeKind.ANY_OBJECT);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.ANY_OBJECT, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("cn", intAttrName.getSchema().getKey());
        assertEquals(SchemaType.DERIVED, intAttrName.getSchemaType());
        assertTrue(intAttrName.getSchema() instanceof DerSchema);
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());

        intAttrName = intAttrNameParser.parse("rvirtualdata", AnyTypeKind.ANY_OBJECT);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.ANY_OBJECT, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("rvirtualdata", intAttrName.getSchema().getKey());
        assertEquals(SchemaType.VIRTUAL, intAttrName.getSchemaType());
        assertTrue(intAttrName.getSchema() instanceof VirSchema);
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());
    }

    @Test
    public void enclosingGroup() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("groups[readers].cn", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.GROUP, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("cn", intAttrName.getSchema().getKey());
        assertEquals(SchemaType.DERIVED, intAttrName.getSchemaType());
        assertTrue(intAttrName.getSchema() instanceof DerSchema);
        assertEquals("readers", intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());
    }

    @Test
    public void relatedUser() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("users[bellini].firstname", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("firstname", intAttrName.getSchema().getKey());
        assertEquals(SchemaType.PLAIN, intAttrName.getSchemaType());
        assertTrue(intAttrName.getSchema() instanceof PlainSchema);
        assertEquals("bellini", intAttrName.getRelatedUser());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
    }

    @Test
    public void relatedAnyObject() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("anyObjects[hp].name", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.ANY_OBJECT, intAttrName.getAnyTypeKind());
        assertEquals("name", intAttrName.getField());
        assertNull(intAttrName.getSchema());
        assertNull(intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertEquals("hp", intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());
    }

    @Test
    public void membership() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("memberships[top].cn", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("cn", intAttrName.getSchema().getKey());
        assertEquals(SchemaType.DERIVED, intAttrName.getSchemaType());
        assertTrue(intAttrName.getSchema() instanceof DerSchema);
        assertNull(intAttrName.getEnclosingGroup());
        assertEquals("top", intAttrName.getMembershipOfGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());
    }

    @Test
    public void relationship() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse(
                "relationships[inclusion][PRINTER].location", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.ANY_OBJECT, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertEquals("location", intAttrName.getSchema().getKey());
        assertEquals(SchemaType.PLAIN, intAttrName.getSchemaType());
        assertTrue(intAttrName.getSchema() instanceof PlainSchema);
        assertEquals("inclusion", intAttrName.getRelationshipType());
        assertEquals("PRINTER", intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertNull(intAttrName.getRelatedUser());
    }

    @Test
    public void invalid() {
        try {
            intAttrNameParser.parse("memberships.cn", AnyTypeKind.USER);
            fail("This should not happen");
        } catch (ParseException e) {
            assertNotNull(e);
        }
    }
}
