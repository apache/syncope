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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PlainSchemaTest extends AbstractTest {

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void findAll() {
        List<? extends PlainSchema> schemas = plainSchemaDAO.findAll();
        assertEquals(28, schemas.size());
    }

    @Test
    public void findByIdLike() {
        List<? extends PlainSchema> schemas = plainSchemaDAO.findByIdLike("fullna%");
        assertEquals(1, schemas.size());
        assertEquals(0, schemas.getFirst().getLabels().size());
    }

    @Test
    public void findByName() {
        PlainSchema schema = plainSchemaDAO.findById("firstname").orElseThrow();

        assertEquals(3, schema.getLabels().size());
        assertTrue(schema.getLabel(Locale.ITALIAN).isPresent());
        assertFalse(schema.getLabel(Locale.KOREAN).isPresent());
    }

    @Test
    public void hasAttrs() {
        PlainSchema schema = plainSchemaDAO.findById("icon").orElseThrow();
        assertTrue(plainSchemaDAO.hasAttrs(schema));

        schema = plainSchemaDAO.findById("aLong").orElseThrow();
        assertFalse(plainSchemaDAO.hasAttrs(schema));
    }

    @Test
    public void existsPlainAttrUniqueValue() {
        PlainAttrValue value = new PlainAttrValue();
        value.setStringValue("rossini@apache.org");
        PlainAttr attr = new PlainAttr();
        attr.setSchema("userId");
        attr.setUniqueValue(value);

        assertFalse(plainSchemaDAO.existsPlainAttrUniqueValue(
                anyUtilsFactory.getInstance(AnyTypeKind.USER),
                "1417acbe-cbf6-4277-9372-e75e04f97000",
                plainSchemaDAO.findById("userId").orElseThrow(),
                attr.getUniqueValue()));
        assertTrue(plainSchemaDAO.existsPlainAttrUniqueValue(
                anyUtilsFactory.getInstance(AnyTypeKind.USER),
                UUID.randomUUID().toString(),
                plainSchemaDAO.findById("userId").orElseThrow(),
                attr.getUniqueValue()));

        value.setStringValue("none@apache.org");
        assertFalse(plainSchemaDAO.existsPlainAttrUniqueValue(
                anyUtilsFactory.getInstance(AnyTypeKind.USER),
                "1417acbe-cbf6-4277-9372-e75e04f97000",
                plainSchemaDAO.findById("userId").orElseThrow(),
                attr.getUniqueValue()));
    }

    @Test
    public void save() {
        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey("secondaryEmail");
        schema.setType(AttrSchemaType.String);
        schema.setValidator(implementationDAO.findById("EmailAddressValidator").orElseThrow());
        schema.setMandatoryCondition("false");
        schema.setMultivalue(true);

        plainSchemaDAO.save(schema);

        PlainSchema actual = plainSchemaDAO.findById("secondaryEmail").orElseThrow();
        assertEquals(schema, actual);
    }

    @Test
    public void saveNonValid() {
        assertThrows(InvalidEntityException.class, () -> {
            PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
            schema.setKey("secondaryEmail");
            schema.setType(AttrSchemaType.String);
            schema.setValidator(implementationDAO.findById("EmailAddressValidator").orElseThrow());
            schema.setMandatoryCondition("false");
            schema.setMultivalue(true);
            schema.setUniqueConstraint(true);

            plainSchemaDAO.save(schema);
        });
    }

    @Test
    public void checkForEnumType() {
        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setType(AttrSchemaType.Enum);
        schema.setKey("color");

        try {
            plainSchemaDAO.save(schema);
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        schema.getEnumValues().put("1", "read");
        schema.getEnumValues().put("2", "yellow");

        plainSchemaDAO.save(schema);

        PlainSchema actual = plainSchemaDAO.findById(schema.getKey()).orElseThrow();
        assertNotNull(actual.getEnumValues());
        assertEquals(2, actual.getEnumValues().size());
    }

    @Test
    public void checkForDropdownType() {
        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setType(AttrSchemaType.Dropdown);
        schema.setKey("dropdown");

        try {
            plainSchemaDAO.save(schema);
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        schema.setDropdownValueProvider(implementationDAO.findById("TestDropdownValueProvider").orElseThrow());

        plainSchemaDAO.save(schema);

        PlainSchema actual = plainSchemaDAO.findById(schema.getKey()).orElseThrow();
        assertNotNull(actual.getEnumValues());
        assertEquals("TestDropdownValueProvider", actual.getDropdownValueProvider().getKey());
    }

    @Test
    public void saveInvalidSchema() {
        assertThrows(InvalidEntityException.class, () -> {
            PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
            schema.setKey("username");
            plainSchemaDAO.save(schema);
        });
    }

    @Test
    public void delete() {
        PlainSchema firstname = plainSchemaDAO.findById("firstname").orElseThrow();

        plainSchemaDAO.deleteById(firstname.getKey());

        assertTrue(plainSchemaDAO.findById("firstname").isEmpty());
    }

    @Test
    public void issueSYNCOPE418() {
        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            plainSchemaDAO.save(schema);
            fail("This should not happen");
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidKey));
        }
    }
}
