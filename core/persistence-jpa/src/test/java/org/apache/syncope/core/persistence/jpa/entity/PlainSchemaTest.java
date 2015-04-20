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
package org.apache.syncope.core.persistence.jpa.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UPlainSchema;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PlainSchemaTest extends AbstractTest {

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Test
    public void findAll() {
        List<UPlainSchema> userList = plainSchemaDAO.findAll(UPlainSchema.class);
        assertEquals(15, userList.size());

        List<GPlainSchema> groupList = plainSchemaDAO.findAll(GPlainSchema.class);
        assertEquals(5, groupList.size());
    }

    @Test
    public void findByName() {
        UPlainSchema schema = plainSchemaDAO.find("fullname", UPlainSchema.class);
        assertNotNull("did not find expected attribute schema", schema);
    }

    @Test
    public void findAttrs() {
        List<GPlainSchema> schemas = plainSchemaDAO.findAll(GPlainSchema.class);
        assertNotNull(schemas);
        assertFalse(schemas.isEmpty());

        for (GPlainSchema schema : schemas) {
            List<GPlainAttr> attrs = plainSchemaDAO.findAttrs(schema, GPlainAttr.class);
            assertNotNull(attrs);
            assertFalse(attrs.isEmpty());
        }
    }

    @Test
    public void save() {
        UPlainSchema schema = entityFactory.newEntity(UPlainSchema.class);
        schema.setKey("secondaryEmail");
        schema.setType(AttrSchemaType.String);
        schema.setValidatorClass("org.apache.syncope.core.validation.EmailAddressValidator");
        schema.setMandatoryCondition("false");
        schema.setMultivalue(true);

        plainSchemaDAO.save(schema);

        UPlainSchema actual = plainSchemaDAO.find("secondaryEmail", UPlainSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(schema, actual);
    }

    @Test(expected = InvalidEntityException.class)
    public void saveNonValid() {
        UPlainSchema schema = entityFactory.newEntity(UPlainSchema.class);
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
        GPlainSchema schema = entityFactory.newEntity(GPlainSchema.class);
        schema.setType(AttrSchemaType.Enum);
        schema.setKey("color");

        Exception ex = null;
        try {
            plainSchemaDAO.save(schema);
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex);

        schema.setEnumerationValues("red" + SyncopeConstants.ENUM_VALUES_SEPARATOR + "yellow");
        schema.setEnumerationKeys("1" + SyncopeConstants.ENUM_VALUES_SEPARATOR + "2");

        plainSchemaDAO.save(schema);

        GPlainSchema actual = plainSchemaDAO.find(schema.getKey(), GPlainSchema.class);
        assertNotNull(actual);
        assertNotNull(actual.getEnumerationKeys());
        assertFalse(actual.getEnumerationKeys().isEmpty());
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidSchema() {
        UPlainSchema schema = entityFactory.newEntity(UPlainSchema.class);
        schema.setKey("username");
        plainSchemaDAO.save(schema);
    }

    @Test
    public void delete() {
        UPlainSchema fullnam = plainSchemaDAO.find("fullname", UPlainSchema.class);

        plainSchemaDAO.delete(fullnam.getKey(), attrUtilsFactory.getInstance(AttributableType.USER));

        UPlainSchema actual = plainSchemaDAO.find("fullname", UPlainSchema.class);
        assertNull("delete did not work", actual);
    }

    @Test
    public void issueSYNCOPE418() {
        UPlainSchema schema = entityFactory.newEntity(UPlainSchema.class);
        schema.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            plainSchemaDAO.save(schema);
            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidName));
        }
    }
}
