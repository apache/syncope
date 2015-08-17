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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class PlainAttrTest extends AbstractTest {

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Autowired
    private PlainAttrValueDAO plainAttrValueDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Test
    public void deleteAttribute() {
        plainAttrDAO.delete(117L, UPlainAttr.class);

        plainAttrDAO.flush();

        assertNull(plainAttrDAO.find(117L, UPlainAttr.class));
        assertNull(plainAttrValueDAO.find(28L, UPlainAttrValue.class));
    }

    @Test
    public void deleteAttributeValue() {
        UPlainAttrValue value = plainAttrValueDAO.find(14L, UPlainAttrValue.class);
        int attributeValueNumber = value.getAttr().getValues().size();

        plainAttrValueDAO.delete(value.getKey(), UPlainAttrValue.class);

        plainAttrValueDAO.flush();

        assertNull(plainAttrValueDAO.find(value.getKey(), UPlainAttrValue.class));

        UPlainAttr attribute = plainAttrDAO.find(104L, UPlainAttr.class);
        assertEquals(attribute.getValues().size(), attributeValueNumber - 1);
    }

    @Test
    public void checkForEnumType() {
        User user = userDAO.find(1L);
        user.setPassword("password123", CipherAlgorithm.SHA);
        assertNotNull(user);

        AnyTypeClass other = anyTypeClassDAO.find("other");

        PlainSchema color = entityFactory.newEntity(PlainSchema.class);
        color.setType(AttrSchemaType.Enum);
        color.setKey("color");
        color.setEnumerationValues("red" + SyncopeConstants.ENUM_VALUES_SEPARATOR + "yellow");

        color = plainSchemaDAO.save(color);

        other.add(color);
        color.setAnyTypeClass(other);

        plainSchemaDAO.flush();

        color = plainSchemaDAO.find("color");
        assertNotNull("expected save to work", color);
        assertEquals(other, color.getAnyTypeClass());

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(color);
        attr.add("yellow", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        userDAO.save(user);
        userDAO.flush();

        user = userDAO.find(1L);
        assertNotNull(user);
        assertNotNull(user.getPlainAttr(color.getKey()));
        assertNotNull(user.getPlainAttr(color.getKey()).getValues());
        assertEquals(user.getPlainAttr(color.getKey()).getValues().size(), 1);
    }

    @Test
    public void derAttrFromSpecialAttrs() {
        AnyTypeClass other = anyTypeClassDAO.find("other");

        DerSchema sderived = entityFactory.newEntity(DerSchema.class);
        sderived.setKey("sderived");
        sderived.setExpression("username + ' - ' + creationDate + '[' + failedLogins + ']'");

        sderived = derSchemaDAO.save(sderived);

        derSchemaDAO.flush();

        other.add(sderived);
        sderived.setAnyTypeClass(other);

        derSchemaDAO.flush();

        sderived = derSchemaDAO.find("sderived");
        assertNotNull("expected save to work", sderived);
        assertEquals(other, sderived.getAnyTypeClass());

        User owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derAttr = entityFactory.newEntity(UDerAttr.class);
        derAttr.setOwner(owner);
        derAttr.setSchema(sderived);

        derAttr = derAttrDAO.save(derAttr);
        derAttrDAO.flush();

        derAttr = derAttrDAO.find(derAttr.getKey(), UDerAttr.class);
        assertNotNull("expected save to work", derAttr);

        String value = derAttr.getValue(owner.getPlainAttrs());
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.startsWith("vivaldi - 2010-10-20"));
        assertTrue(value.endsWith("[0]"));
    }
}
