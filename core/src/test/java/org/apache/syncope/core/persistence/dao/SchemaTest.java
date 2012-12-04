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
package org.apache.syncope.core.persistence.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.beans.role.RSchema;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.AbstractTest;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.SchemaType;
import org.apache.syncope.validation.InvalidEntityException;

@Transactional
public class SchemaTest extends AbstractTest {

    @Autowired
    private SchemaDAO schemaDAO;

    @Test
    public void findAll() {
        List<USchema> userList = schemaDAO.findAll(USchema.class);
        assertEquals(12, userList.size());

        List<RSchema> roleList = schemaDAO.findAll(RSchema.class);
        assertEquals(5, roleList.size());
    }

    @Test
    public void findByName() {
        USchema attributeSchema = schemaDAO.find("fullname", USchema.class);
        assertNotNull("did not find expected attribute schema", attributeSchema);
    }

    @Test
    public void getAttributes() {
        List<RSchema> schemas = schemaDAO.findAll(RSchema.class);
        assertNotNull(schemas);
        assertFalse(schemas.isEmpty());

        List<RAttr> attrs;
        for (RSchema schema : schemas) {
            attrs = schemaDAO.getAttributes(schema, RAttr.class);
            assertNotNull(attrs);
            assertFalse(attrs.isEmpty());
        }
    }

    @Test
    public void save() {
        USchema attributeSchema = new USchema();
        attributeSchema.setName("secondaryEmail");
        attributeSchema.setType(SchemaType.String);
        attributeSchema.setValidatorClass("org.apache.syncope.core.validation.EmailAddressValidator");
        attributeSchema.setMandatoryCondition("false");
        attributeSchema.setMultivalue(true);

        schemaDAO.save(attributeSchema);

        USchema actual = schemaDAO.find("secondaryEmail", USchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(attributeSchema, actual);
    }

    @Test(expected = InvalidEntityException.class)
    public void saveNonValid() {
        USchema attributeSchema = new USchema();
        attributeSchema.setName("secondaryEmail");
        attributeSchema.setType(SchemaType.String);
        attributeSchema.setValidatorClass("org.apache.syncope.core.validation.EmailAddressValidator");
        attributeSchema.setMandatoryCondition("false");
        attributeSchema.setMultivalue(true);
        attributeSchema.setUniqueConstraint(true);

        schemaDAO.save(attributeSchema);
    }

    @Test
    public void checkForEnumType() {
        RSchema schema = new RSchema();
        schema.setType(SchemaType.Enum);
        schema.setName("color");

        Exception ex = null;
        try {
            schemaDAO.save(schema);
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex);

        schema.setEnumerationValues("red" + AbstractSchema.enumValuesSeparator + "yellow");
        schema.setEnumerationKeys("1" + AbstractSchema.enumValuesSeparator + "2");

        schemaDAO.save(schema);

        RSchema actual = schemaDAO.find(schema.getName(), RSchema.class);
        assertNotNull(actual);
        assertNotNull(actual.getEnumerationKeys());
        assertFalse(actual.getEnumerationKeys().isEmpty());
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidSchema() {
        USchema schema = new USchema();
        schema.setName("username");
        schemaDAO.save(schema);
    }

    @Test
    public void delete() {
        USchema schema = schemaDAO.find("fullname", USchema.class);

        schemaDAO.delete(schema.getName(), AttributableUtil.getInstance(AttributableType.USER));

        USchema actual = schemaDAO.find("fullname", USchema.class);
        assertNull("delete did not work", actual);
    }
}
