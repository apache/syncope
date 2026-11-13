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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SchemaDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private SchemaDataBinder binder;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Test
    void getPlainSchemaTO() {
        PlainSchemaTO fullname = binder.getPlainSchemaTO("fullname");
        assertEquals("fullname", fullname.getKey());
        assertEquals(AttrSchemaType.String, fullname.getType());
        assertEquals("true", fullname.getMandatoryCondition());
        assertTrue(fullname.isUniqueConstraint());
        assertFalse(fullname.isMultivalue());
        assertFalse(fullname.isReadonly());
        assertEquals("minimal user", fullname.getAnyTypeClass());

        assertEquals("yyyy-MM-dd", binder.getPlainSchemaTO("loginDate").getConversionPattern());
        assertEquals("TestDropdownValueProvider", binder.getPlainSchemaTO("dd").getDropdownValueProvider());
        assertEquals("EmailAddressValidator", binder.getPlainSchemaTO("email").getValidator());

        PlainSchemaTO gender = binder.getPlainSchemaTO("gender");
        assertEquals(AttrSchemaType.Enum, gender.getType());
        assertEquals(Map.of("M", "Male", "F", "Female"), gender.getEnumValues());

        PlainSchemaTO obscure = binder.getPlainSchemaTO("obscure");
        assertEquals(AttrSchemaType.Encrypted, obscure.getType());
        assertEquals("SHA", obscure.getCipherAlgorithm().name());
        assertEquals("7abcdefghilmnopqrstuvz9#", obscure.getSecretKey());

        assertThrows(NotFoundException.class, () -> binder.getPlainSchemaTO("not-existing"));
    }

    @Test
    void getDerSchemaTO() {
        DerSchemaTO cn = binder.getDerSchemaTO("cn");
        assertEquals("cn", cn.getKey());
        assertEquals("surname + ', ' + firstname", cn.getExpression());
        assertEquals("minimal user", cn.getAnyTypeClass());

        assertThrows(NotFoundException.class, () -> binder.getDerSchemaTO("not-existing"));
    }

    @Test
    void createUpdatePlain() {
        PlainSchemaTO plainSchemaTO = new PlainSchemaTO();
        plainSchemaTO.setKey("testPlain");
        plainSchemaTO.setType(AttrSchemaType.String);
        plainSchemaTO.setMandatoryCondition("false");
        plainSchemaTO.setAnyTypeClass("other");

        PlainSchema plainSchema = plainSchemaDAO.save(binder.create(plainSchemaTO));

        PlainSchemaTO created = binder.getPlainSchemaTO("testPlain");
        assertEquals(AttrSchemaType.String, created.getType());
        assertEquals("other", created.getAnyTypeClass());

        created.setType(AttrSchemaType.Long);
        created.setMultivalue(true);
        binder.update(created, plainSchema);

        PlainSchemaTO updated = binder.getPlainSchemaTO("testPlain");
        assertEquals(AttrSchemaType.Long, updated.getType());
        assertTrue(updated.isMultivalue());

        updated.setAnyTypeClass(null);
        binder.update(updated, plainSchema);
        assertNull(binder.getPlainSchemaTO("testPlain").getAnyTypeClass());
    }

    @Test
    void updatePlainWithAttrs() {
        PlainSchemaTO fullname = binder.getPlainSchemaTO("fullname");
        PlainSchema schema = plainSchemaDAO.findById("fullname").orElseThrow();

        fullname.setType(AttrSchemaType.Long);
        assertThrows(SyncopeClientException.class, () -> binder.update(fullname, schema));

        fullname.setType(AttrSchemaType.String);
        fullname.setUniqueConstraint(false);
        assertThrows(SyncopeClientException.class, () -> binder.update(fullname, schema));
    }

    @Test
    void invalidPlain() {
        PlainSchemaTO invalidMandatoryCondition = new PlainSchemaTO();
        invalidMandatoryCondition.setKey("testPlain");
        invalidMandatoryCondition.setType(AttrSchemaType.String);
        invalidMandatoryCondition.setMandatoryCondition("1 +");
        assertThrows(SyncopeClientException.class, () -> binder.create(invalidMandatoryCondition));
    }

    @Test
    void createUpdateDer() {
        DerSchemaTO derSchemaTO = new DerSchemaTO();
        derSchemaTO.setKey("testDer");
        derSchemaTO.setExpression("surname + ', ' + firstname");
        derSchemaTO.setAnyTypeClass("other");

        DerSchema derSchema = derSchemaDAO.save(binder.create(derSchemaTO));

        DerSchemaTO created = binder.getDerSchemaTO("testDer");
        assertEquals("surname + ', ' + firstname", created.getExpression());
        assertEquals("other", created.getAnyTypeClass());

        created.setExpression("firstname + ' ' + surname");
        binder.update(created, derSchema);
        assertEquals("firstname + ' ' + surname", binder.getDerSchemaTO("testDer").getExpression());

        created.setAnyTypeClass(null);
        binder.update(created, derSchema);
        assertNull(binder.getDerSchemaTO("testDer").getAnyTypeClass());
    }

    @Test
    void invalidDer() {
        DerSchemaTO blankExpression = new DerSchemaTO();
        blankExpression.setKey("testDer");
        assertThrows(SyncopeClientException.class, () -> binder.create(blankExpression));

        DerSchemaTO invalidExpression = new DerSchemaTO();
        invalidExpression.setKey("testDer");
        invalidExpression.setExpression("surname +");
        assertThrows(SyncopeClientException.class, () -> binder.create(invalidExpression));
    }
}
