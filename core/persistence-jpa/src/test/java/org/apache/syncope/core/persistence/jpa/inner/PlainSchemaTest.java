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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class PlainSchemaTest extends AbstractTest {

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Test
    public void findAll() {
        List<PlainSchema> schemas = plainSchemaDAO.findAll();
        assertEquals(43, schemas.size());
    }

    @Test
    public void findByName() {
        PlainSchema schema = plainSchemaDAO.find("fullname");
        assertNotNull("did not find expected attribute schema", schema);
    }

    @Test
    public void findAttrs() {
        PlainSchema schema = plainSchemaDAO.find("icon");
        assertNotNull(schema);

        List<GPlainAttr> attrs = plainSchemaDAO.findAttrs(schema, GPlainAttr.class);
        assertNotNull(attrs);
        assertFalse(attrs.isEmpty());
    }

    @Test
    public void save() {
        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey("secondaryEmail");
        schema.setType(AttrSchemaType.String);
        schema.setValidatorClass("org.apache.syncope.core.validation.EmailAddressValidator");
        schema.setMandatoryCondition("false");
        schema.setMultivalue(true);

        plainSchemaDAO.save(schema);

        PlainSchema actual = plainSchemaDAO.find("secondaryEmail");
        assertNotNull("expected save to work", actual);
        assertEquals(schema, actual);
    }

    @Test(expected = InvalidEntityException.class)
    public void saveNonValid() {
        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey("secondaryEmail");
        schema.setType(AttrSchemaType.String);
        schema.setValidatorClass("org.apache.syncope.core.validation.EmailAddressValidator");
        schema.setMandatoryCondition("false");
        schema.setMultivalue(true);
        schema.setUniqueConstraint(true);

        plainSchemaDAO.save(schema);
    }

    @Test
    public void checkForEnumType() {
        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setType(AttrSchemaType.Enum);
        schema.setKey("color");

        try {
            plainSchemaDAO.save(schema);
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        schema.setEnumerationValues("red" + SyncopeConstants.ENUM_VALUES_SEPARATOR + "yellow");
        schema.setEnumerationKeys("1" + SyncopeConstants.ENUM_VALUES_SEPARATOR + "2");

        plainSchemaDAO.save(schema);

        PlainSchema actual = plainSchemaDAO.find(schema.getKey());
        assertNotNull(actual);
        assertNotNull(actual.getEnumerationKeys());
        assertFalse(actual.getEnumerationKeys().isEmpty());
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidSchema() {
        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey("username");
        plainSchemaDAO.save(schema);
    }

    @Test
    public void delete() {
        PlainSchema firstname = plainSchemaDAO.find("firstname");

        plainSchemaDAO.delete(firstname.getKey());

        PlainSchema actual = plainSchemaDAO.find("firstname");
        assertNull("delete did not work", actual);
    }

    @Test
    public void issueSYNCOPE418() {
        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            plainSchemaDAO.save(schema);
            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidKey));
        }
    }
}
