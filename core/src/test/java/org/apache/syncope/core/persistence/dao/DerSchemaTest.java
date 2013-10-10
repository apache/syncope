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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.role.RDerSchema;
import org.apache.syncope.core.persistence.beans.user.UDerSchema;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.apache.syncope.core.util.AttributableUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DerSchemaTest extends AbstractDAOTest {

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Test
    public void findAll() {
        List<UDerSchema> list = derSchemaDAO.findAll(UDerSchema.class);
        assertEquals(3, list.size());
    }

    @Test
    public void findByName() {
        UDerSchema attributeSchema = derSchemaDAO.find("cn", UDerSchema.class);
        assertNotNull("did not find expected derived attribute schema", attributeSchema);
    }

    @Test
    public void save() {
        UDerSchema derivedAttributeSchema = new UDerSchema();
        derivedAttributeSchema.setName("cn2");
        derivedAttributeSchema.setExpression("firstname surname");

        derSchemaDAO.save(derivedAttributeSchema);

        UDerSchema actual = derSchemaDAO.find("cn2", UDerSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttributeSchema, actual);
    }

    @Test
    public void delete() {
        UDerSchema cn = derSchemaDAO.find("cn", UDerSchema.class);
        assertNotNull(cn);

        derSchemaDAO.delete(cn.getName(), AttributableUtil.getInstance(AttributableType.USER));

        AbstractDerSchema actual = derSchemaDAO.find("cn", UDerSchema.class);
        assertNull("delete did not work", actual);

        // ------------- //

        RDerSchema rderiveddata = derSchemaDAO.find("rderiveddata", RDerSchema.class);
        assertNotNull(rderiveddata);

        derSchemaDAO.delete(rderiveddata.getName(), AttributableUtil.getInstance(AttributableType.ROLE));

        actual = derSchemaDAO.find("rderiveddata", RDerSchema.class);
        assertNull("delete did not work", actual);
    }

    @Test
    public void issueSYNCOPE418() {
        UDerSchema schema = new UDerSchema();
        schema.setName("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            derSchemaDAO.save(schema);
            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidName));
        }
    }
}
