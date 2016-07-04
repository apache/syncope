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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class DerSchemaTest extends AbstractTest {

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Test
    public void findAll() {
        List<DerSchema> list = derSchemaDAO.findAll();
        assertEquals(10, list.size());
    }

    @Test
    public void findByName() {
        DerSchema attributeSchema = derSchemaDAO.find("cn");
        assertNotNull("did not find expected derived attribute schema", attributeSchema);
    }

    @Test
    public void save() {
        DerSchema derivedAttributeSchema = entityFactory.newEntity(DerSchema.class);
        derivedAttributeSchema.setKey("cn2");
        derivedAttributeSchema.setExpression("firstname surname");

        derSchemaDAO.save(derivedAttributeSchema);

        DerSchema actual = derSchemaDAO.find("cn2");
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttributeSchema, actual);
    }

    @Test
    public void delete() {
        DerSchema cn = derSchemaDAO.find("cn");
        assertNotNull(cn);

        derSchemaDAO.delete(cn.getKey());

        DerSchema actual = derSchemaDAO.find("cn");
        assertNull("delete did not work", actual);

        // ------------- //
        DerSchema rderiveddata = derSchemaDAO.find("rderiveddata");
        assertNotNull(rderiveddata);

        derSchemaDAO.delete(rderiveddata.getKey());

        actual = derSchemaDAO.find("rderiveddata");
        assertNull("delete did not work", actual);
    }

    @Test
    public void issueSYNCOPE418() {
        DerSchema schema = entityFactory.newEntity(DerSchema.class);
        schema.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            derSchemaDAO.save(schema);
            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidKey));
        }
    }
}
