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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class IntAttrNameParserTest extends AbstractTest {

    private static final Map<AnyTypeKind, List<String>> ANY_FIELDS = Map.of(
            AnyTypeKind.USER, List.of("key", "username", "uManager", "gManager"),
            AnyTypeKind.GROUP, List.of("key", "name", "uManager", "gManager"),
            AnyTypeKind.ANY_OBJECT, List.of("key", "name", "uManager", "gManager"));

    private static final List<String> REALM_FIELDS = List.of("key", "name");

    @Mock
    private PlainSchemaDAO plainSchemaDAO;

    @Mock
    private DerSchemaDAO derSchemaDAO;

    @Mock
    private AnyUtilsFactory anyUtilsFactory;

    @Mock
    private AnyUtils anyUtils;

    @Mock
    private RealmUtils realmUtils;

    private IntAttrNameParser intAttrNameParser;

    @BeforeEach
    public void initMocks() throws NoSuchFieldException {
        lenient().when(anyUtilsFactory.getInstance(any(AnyTypeKind.class))).thenAnswer(ic -> {
            when(anyUtils.anyTypeKind()).thenReturn(ic.getArgument(0));
            return anyUtils;
        });
        lenient().when(anyUtils.getField(anyString())).thenAnswer(ic -> {
            String fieldName = ic.getArgument(0);
            if (ANY_FIELDS.get(anyUtils.anyTypeKind()).contains(fieldName)) {
                Field field = mock(Field.class);
                when(field.getName()).thenReturn(fieldName);
                when(field.getType()).thenAnswer(ic2 -> String.class);
                return Optional.of(field);
            }
            return Optional.empty();
        });
        lenient().when(realmUtils.getField(anyString())).thenAnswer(ic -> {
            String fieldName = ic.getArgument(0);
            if (REALM_FIELDS.contains(fieldName)) {
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
                case "email", "firstname", "location", "index", "user.valueWithDot" -> {
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

        intAttrNameParser = new IntAttrNameParser(plainSchemaDAO, derSchemaDAO, anyUtilsFactory, realmUtils);
    }

    @Test
    public void ownFields() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("key", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertNotNull(intAttrName.getField());
        assertEquals("key", intAttrName.getField());
        assertNull(intAttrName.getSchemaInfo());
        assertNull(intAttrName.getExternalGroup());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());

        intAttrName = intAttrNameParser.parse("name", AnyTypeKind.GROUP);
        assertNotNull(intAttrName);
        assertNotNull(intAttrName.getField());
        assertEquals("name", intAttrName.getField());
        assertNull(intAttrName.getSchemaInfo());
        assertNull(intAttrName.getExternalGroup());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());

        intAttrName = intAttrNameParser.parse("uManager", AnyTypeKind.GROUP);
        assertNotNull(intAttrName);
        assertNotNull(intAttrName.getField());
        assertEquals("uManager", intAttrName.getField());
        assertNull(intAttrName.getSchemaInfo());
        assertNull(intAttrName.getExternalGroup());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());

        intAttrName = intAttrNameParser.parse("name", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertNull(intAttrName.getField());

        Object nullObj = null;
        int expected = new HashCodeBuilder().
                append(nullObj).append(nullObj).append(nullObj).
                append(nullObj).append(nullObj).append(nullObj).append(nullObj).
                build();
        assertEquals(expected, intAttrName.hashCode());
        IntAttrName intAttrName2 = intAttrNameParser.parse("email", AnyTypeKind.USER);
        assertFalse(intAttrName.equals(intAttrName2));
        assertFalse(intAttrName.equals(nullObj));
        assertTrue(intAttrName.equals(intAttrName));
        String toString = intAttrName.toString();
        assertTrue(toString.startsWith("org.apache.syncope.core.provisioning.api.IntAttrName"));
        assertTrue(toString.endsWith("[<null>,<null>,<null>,<null>,<null>,<null>,<null>]"));
    }

    @Test
    public void ownSchema() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("email", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertNull(intAttrName.getField());
        assertEquals("email", intAttrName.getSchemaInfo().schema().getKey());
        assertEquals(SchemaType.PLAIN, intAttrName.getSchemaInfo().type());
        assertTrue(intAttrName.getSchemaInfo().schema() instanceof PlainSchema);
        assertNull(intAttrName.getExternalGroup());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());

        intAttrName = intAttrNameParser.parse("cn", AnyTypeKind.ANY_OBJECT);
        assertNotNull(intAttrName);
        assertNull(intAttrName.getField());
        assertEquals("cn", intAttrName.getSchemaInfo().schema().getKey());
        assertEquals(SchemaType.DERIVED, intAttrName.getSchemaInfo().type());
        assertTrue(intAttrName.getSchemaInfo().schema() instanceof DerSchema);
        assertNull(intAttrName.getExternalGroup());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());
    }

    @Test
    public void externalGroup() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("groups[readers].cn", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertNull(intAttrName.getField());
        assertEquals("cn", intAttrName.getSchemaInfo().schema().getKey());
        assertEquals(SchemaType.DERIVED, intAttrName.getSchemaInfo().type());
        assertTrue(intAttrName.getSchemaInfo().schema() instanceof DerSchema);
        assertEquals("readers", intAttrName.getExternalGroup());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());
    }

    @Test
    public void externalUser() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("users[bellini].firstname", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertNull(intAttrName.getField());
        assertEquals("firstname", intAttrName.getSchemaInfo().schema().getKey());
        assertEquals(SchemaType.PLAIN, intAttrName.getSchemaInfo().type());
        assertTrue(intAttrName.getSchemaInfo().schema() instanceof PlainSchema);
        assertEquals("bellini", intAttrName.getExternalUser());
        assertNull(intAttrName.getExternalGroup());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
    }

    @Test
    public void externalAnyObject() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("anyObjects[hp].name", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals("name", intAttrName.getField());
        assertNull(intAttrName.getSchemaInfo());
        assertNull(intAttrName.getExternalGroup());
        assertEquals("hp", intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());
    }

    @Test
    public void membership() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("memberships[top].cn", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertNull(intAttrName.getField());
        assertEquals("cn", intAttrName.getSchemaInfo().schema().getKey());
        assertEquals(SchemaType.DERIVED, intAttrName.getSchemaInfo().type());
        assertTrue(intAttrName.getSchemaInfo().schema() instanceof DerSchema);
        assertNull(intAttrName.getExternalGroup());
        assertEquals("top", intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());
    }

    @Test
    public void relationship() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("relationships[inclusion][hp].location", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertNull(intAttrName.getField());
        assertEquals("location", intAttrName.getSchemaInfo().schema().getKey());
        assertEquals(SchemaType.PLAIN, intAttrName.getSchemaInfo().type());
        assertTrue(intAttrName.getSchemaInfo().schema() instanceof PlainSchema);
        assertEquals("inclusion", intAttrName.getRelationshipInfo().type());
        assertEquals("hp", intAttrName.getRelationshipInfo().anyObject());
        assertNull(intAttrName.getExternalGroup());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getExternalUser());
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

    @Test
    public void realm() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("key");
        assertNotNull(intAttrName);
        assertNotNull(intAttrName.getField());
        assertEquals("key", intAttrName.getField());
        assertNull(intAttrName.getSchemaInfo());
        assertNull(intAttrName.getExternalGroup());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());

        intAttrName = intAttrNameParser.parse("name");
        assertNotNull(intAttrName);
        assertNotNull(intAttrName.getField());
        assertEquals("name", intAttrName.getField());
        assertNull(intAttrName.getSchemaInfo());
        assertNull(intAttrName.getExternalGroup());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());

        intAttrName = intAttrNameParser.parse("index");
        assertNotNull(intAttrName);
        assertNull(intAttrName.getField());
        assertEquals("index", intAttrName.getSchemaInfo().schema().getKey());
        assertEquals(SchemaType.PLAIN, intAttrName.getSchemaInfo().type());
        assertTrue(intAttrName.getSchemaInfo().schema() instanceof PlainSchema);
        assertNull(intAttrName.getExternalGroup());
        assertNull(intAttrName.getMembership());
        assertNull(intAttrName.getExternalAnyObject());
        assertNull(intAttrName.getRelationshipInfo());
        assertNull(intAttrName.getExternalUser());

        try {
            intAttrNameParser.parse("groups[readers].cn");
            fail("This should not happen");
        } catch (ParseException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void issueSYNCOPE1894() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("user.valueWithDot", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertNull(intAttrName.getField());
        assertEquals("user.valueWithDot", intAttrName.getSchemaInfo().schema().getKey());
    }
}
