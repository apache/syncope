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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.provisioning.api.data.AnyTypeClassDataBinder;
import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnyTypeClassDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private AnyTypeClassDataBinder binder;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private SchemaDataBinder schemaDataBinder;

    @Test
    void get() {
        AnyTypeClassTO user = binder.getAnyTypeClassTO(anyTypeClassDAO.findById("minimal user").orElseThrow());
        assertEquals("minimal user", user.getKey());
        assertTrue(user.getPlainSchemas().containsAll(List.of("fullname", "userId", "firstname", "surname", "email")));
        assertTrue(user.getDerSchemas().containsAll(List.of("cn", "info")));
        assertTrue(user.getInUseByTypes().contains(AnyTypeKind.USER.name()));
    }

    @Test
    void createUpdate() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("testSchema");
        schemaTO.setType(AttrSchemaType.String);
        schemaTO.setMultivalue(false);
        schemaTO.setUniqueConstraint(false);
        schemaTO.setReadonly(false);
        PlainSchema schema = plainSchemaDAO.save(schemaDataBinder.create(schemaTO));

        AnyTypeClassTO anyTypeClassTO = new AnyTypeClassTO();
        anyTypeClassTO.setKey("testClass");
        anyTypeClassTO.getPlainSchemas().add("testSchema");
        anyTypeClassTO.getPlainSchemas().add("ctype");
        anyTypeClassTO.getPlainSchemas().add("not-existing");
        anyTypeClassTO.getDerSchemas().add("not-existing");

        AnyTypeClass anyTypeClass = binder.create(anyTypeClassTO);
        anyTypeClass = anyTypeClassDAO.save(anyTypeClass);

        AnyTypeClassTO created = binder.getAnyTypeClassTO(anyTypeClass);
        assertEquals("testClass", created.getKey());
        assertEquals(List.of("testSchema"), created.getPlainSchemas());
        assertTrue(created.getDerSchemas().isEmpty());
        assertEquals(anyTypeClass, plainSchemaDAO.findById("testSchema").orElseThrow().getAnyTypeClass());

        created.getPlainSchemas().clear();
        binder.update(anyTypeClass, created);

        assertTrue(binder.getAnyTypeClassTO(anyTypeClass).getPlainSchemas().isEmpty());
        assertNull(plainSchemaDAO.findById("testSchema").orElseThrow().getAnyTypeClass());
    }
}
