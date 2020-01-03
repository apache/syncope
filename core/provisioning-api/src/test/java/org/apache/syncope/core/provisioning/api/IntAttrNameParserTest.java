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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class IntAttrNameParserTest {

    private static final Map<AnyTypeKind, List<String>> FIELDS = new HashMap<>();

    static {
        FIELDS.put(AnyTypeKind.USER, Arrays.asList("key", "username"));
        FIELDS.put(AnyTypeKind.GROUP, Arrays.asList("key", "name", "userOwner"));
        FIELDS.put(AnyTypeKind.ANY_OBJECT, Arrays.asList("key", "name"));
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

    @InjectMocks
    private IntAttrNameParser intAttrNameParser;

    @BeforeEach
    public void initMocks() throws NoSuchFieldException {
        MockitoAnnotations.initMocks(this);

        when(anyUtilsFactory.getInstance(any(AnyTypeKind.class))).thenAnswer(ic -> {
            when(anyUtils.anyTypeKind()).thenReturn(ic.getArgument(0));
            return anyUtils;
        });
        when(anyUtils.getField(anyString())).thenAnswer(ic -> {
            String field = ic.getArgument(0);
            return FIELDS.get(anyUtils.anyTypeKind()).contains(field)
                    ? ReflectionUtils.findField(getClass(), "anyUtils")
                    : null;
        });
        when(plainSchemaDAO.find(anyString())).thenAnswer(ic -> {
            String schemaName = ic.getArgument(0);
            switch (schemaName) {
                case "email":
                case "firstname":
                case "location":
                    PlainSchema schema = mock(PlainSchema.class);
                    when(schema.getKey()).thenReturn(schemaName);
                    when(schema.getType()).thenReturn(AttrSchemaType.String);
                    return schema;

                default:
                    return null;
            }
        });
        when(derSchemaDAO.find(anyString())).thenAnswer(ic -> {
            String schemaName = ic.getArgument(0);
            switch (schemaName) {
                case "cn":
                    DerSchema schema = mock(DerSchema.class);
                    when(schema.getKey()).thenReturn(ic.getArgument(0));
                    return schema;

                default:
                    return null;
            }
        });
        when(virSchemaDAO.find(anyString())).thenAnswer(ic -> {
            String schemaName = ic.getArgument(0);
            switch (schemaName) {
                case "rvirtualdata":
                    VirSchema schema = mock(VirSchema.class);
                    when(schema.getKey()).thenReturn(ic.getArgument(0));
                    return schema;

                default:
                    return null;
            }
        });
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
        assertNull(intAttrName.getPrivilegesOfApplication());
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
        assertNull(intAttrName.getPrivilegesOfApplication());
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
        assertNull(intAttrName.getPrivilegesOfApplication());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());

        intAttrName = intAttrNameParser.parse("name", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
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
        assertNull(intAttrName.getPrivilegesOfApplication());
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
        assertNull(intAttrName.getPrivilegesOfApplication());
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
        assertNull(intAttrName.getPrivilegesOfApplication());
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
        assertNull(intAttrName.getPrivilegesOfApplication());
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
        assertNull(intAttrName.getPrivilegesOfApplication());
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
        assertNull(intAttrName.getPrivilegesOfApplication());
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
        assertNull(intAttrName.getPrivilegesOfApplication());
        assertNull(intAttrName.getRelationshipAnyType());
        assertNull(intAttrName.getRelationshipType());
        assertNull(intAttrName.getRelatedUser());
    }

    @Test
    public void privileges() throws ParseException {
        IntAttrName intAttrName = intAttrNameParser.parse("privileges[mightyApp]", AnyTypeKind.USER);
        assertNotNull(intAttrName);
        assertEquals(AnyTypeKind.USER, intAttrName.getAnyTypeKind());
        assertNull(intAttrName.getField());
        assertNull(intAttrName.getSchema());
        assertNull(intAttrName.getSchemaType());
        assertNull(intAttrName.getEnclosingGroup());
        assertNull(intAttrName.getRelatedAnyObject());
        assertEquals("mightyApp", intAttrName.getPrivilegesOfApplication());
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
        assertNull(intAttrName.getPrivilegesOfApplication());
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
