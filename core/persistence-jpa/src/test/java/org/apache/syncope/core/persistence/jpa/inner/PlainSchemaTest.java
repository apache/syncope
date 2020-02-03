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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Locale;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class PlainSchemaTest extends AbstractTest {

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void findAll() {
        List<PlainSchema> schemas = plainSchemaDAO.findAll();
        assertEquals(27, schemas.size());
    }

    @Test
    public void search() {
        List<PlainSchema> schemas = plainSchemaDAO.findByKeyword("fullna%");
        assertEquals(1, schemas.size());
        assertEquals(0, schemas.get(0).getLabels().size());
    }

    @Test
    public void findByName() {
        PlainSchema schema = plainSchemaDAO.find("firstname");
        assertNotNull(schema);

        assertEquals(3, schema.getLabels().size());
        assertTrue(schema.getLabel(Locale.ITALIAN).isPresent());
        assertFalse(schema.getLabel(Locale.KOREAN).isPresent());
    }

    @Tag("plainAttrTable")
    @Test
    public void findAttrs() {
        PlainSchema schema = plainSchemaDAO.find("icon");
        assertNotNull(schema);

        List<GPlainAttr> gattrs = plainSchemaDAO.findAttrs(schema, GPlainAttr.class);
        assertNotNull(gattrs);
        assertFalse(gattrs.isEmpty());

        schema = plainSchemaDAO.find("aLong");
        assertNotNull(schema);

        List<UPlainAttr> uattrs = plainSchemaDAO.findAttrs(schema, UPlainAttr.class);
        assertNotNull(uattrs);
        assertTrue(uattrs.isEmpty());
    }

    @Test
    public void hasAttrs() {
        PlainSchema schema = plainSchemaDAO.find("icon");
        assertNotNull(schema);
        assertTrue(plainSchemaDAO.hasAttrs(schema, GPlainAttr.class));

        schema = plainSchemaDAO.find("aLong");
        assertNotNull(schema);
        assertFalse(plainSchemaDAO.hasAttrs(schema, UPlainAttr.class));
    }

    @Test
    public void save() {
        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey("secondaryEmail");
        schema.setType(AttrSchemaType.String);
        schema.setValidator(implementationDAO.find("EmailAddressValidator"));
        schema.setMandatoryCondition("false");
        schema.setMultivalue(true);

        plainSchemaDAO.save(schema);

        PlainSchema actual = plainSchemaDAO.find("secondaryEmail");
        assertNotNull(actual);
        assertEquals(schema, actual);
    }

    @Test
    public void saveNonValid() {
        assertThrows(InvalidEntityException.class, () -> {
            PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
            schema.setKey("secondaryEmail");
            schema.setType(AttrSchemaType.String);
            schema.setValidator(implementationDAO.find("EmailAddressValidator"));
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

        schema.setEnumerationValues("red" + SyncopeConstants.ENUM_VALUES_SEPARATOR + "yellow");
        schema.setEnumerationKeys('1' + SyncopeConstants.ENUM_VALUES_SEPARATOR + '2');

        plainSchemaDAO.save(schema);

        PlainSchema actual = plainSchemaDAO.find(schema.getKey());
        assertNotNull(actual);
        assertNotNull(actual.getEnumerationKeys());
        assertFalse(actual.getEnumerationKeys().isEmpty());
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
        PlainSchema firstname = plainSchemaDAO.find("firstname");

        plainSchemaDAO.delete(firstname.getKey());

        PlainSchema actual = plainSchemaDAO.find("firstname");
        assertNull(actual);
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
