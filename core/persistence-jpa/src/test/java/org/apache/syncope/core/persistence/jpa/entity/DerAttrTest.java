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

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DerAttrTest extends AbstractTest {

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Test
    public void findAll() {
        List<UDerAttr> list = derAttrDAO.findAll(UDerAttr.class);
        assertEquals("did not get expected number of derived attributes ", 2, list.size());
    }

    @Test
    public void findById() {
        UDerAttr attribute = derAttrDAO.find(100L, UDerAttr.class);
        assertNotNull("did not find expected attribute schema", attribute);
    }

    @Test
    public void saveUDerAttribute() {
        DerSchema cnSchema = derSchemaDAO.find("cn");
        assertNotNull(cnSchema);

        User owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derAttr = entityFactory.newEntity(UDerAttr.class);
        derAttr.setOwner(owner);
        derAttr.setSchema(cnSchema);

        derAttr = derAttrDAO.save(derAttr);

        UDerAttr actual = derAttrDAO.find(derAttr.getKey(), UDerAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derAttr, actual);

        UPlainAttrValue firstname = owner.getPlainAttr("firstname").getValues().iterator().next();
        UPlainAttrValue surname = owner.getPlainAttr("surname").getValues().iterator().next();

        assertEquals(surname.getValue() + ", " + firstname.getValue(), derAttr.getValue(owner.getPlainAttrs()));
    }

    @Test
    public void saveGDerAttribute() {
        DerSchema schema = derSchemaDAO.find("rderiveddata");
        assertNotNull(schema);

        Group owner = groupDAO.find(1L);
        assertNotNull("did not get expected user", owner);

        GDerAttr derAttr = entityFactory.newEntity(GDerAttr.class);
        derAttr.setOwner(owner);
        derAttr.setSchema(schema);

        derAttr = derAttrDAO.save(derAttr);

        GDerAttr actual = derAttrDAO.find(derAttr.getKey(), GDerAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derAttr, actual);

        GPlainAttrValue sx = owner.getPlainAttr("rderived_sx").getValues().iterator().next();
        GPlainAttrValue dx = owner.getPlainAttr("rderived_dx").getValues().iterator().next();

        assertEquals(sx.getValue() + "-" + dx.getValue(), derAttr.getValue(owner.getPlainAttrs()));
    }

    @Test
    public void delete() {
        UDerAttr attribute = derAttrDAO.find(100L, UDerAttr.class);
        String schemaName = attribute.getSchema().getKey();

        derAttrDAO.delete(attribute.getKey(), UDerAttr.class);

        UDerAttr actual = derAttrDAO.find(100L, UDerAttr.class);
        assertNull("delete did not work", actual);

        DerSchema attributeSchema = derSchemaDAO.find(schemaName);
        assertNotNull("user derived attribute schema deleted when deleting values", attributeSchema);
    }

    @Test
    public void issueSYNCOPE134User() {
        DerSchema sderived = entityFactory.newEntity(DerSchema.class);
        sderived.setKey("sderived");
        sderived.setExpression("status + ' - ' + username + ' - ' + creationDate + '[' + failedLogins + ']'");

        sderived = derSchemaDAO.save(sderived);
        derSchemaDAO.flush();

        DerSchema actual = derSchemaDAO.find("sderived");
        assertNotNull("expected save to work", actual);
        assertEquals(sderived, actual);

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
        assertTrue(value.startsWith("active - vivaldi - 2010-10-20"));
        assertTrue(value.endsWith("[0]"));
    }

    @Test
    public void issueSYNCOPE134Group() {
        DerSchema sderived = entityFactory.newEntity(DerSchema.class);
        sderived.setKey("sderived");
        sderived.setExpression("name");

        sderived = derSchemaDAO.save(sderived);
        derSchemaDAO.flush();

        DerSchema actual = derSchemaDAO.find("sderived");
        assertNotNull("expected save to work", actual);
        assertEquals(sderived, actual);

        Group owner = groupDAO.find(7L);
        assertNotNull("did not get expected group", owner);

        GDerAttr derAttr = entityFactory.newEntity(GDerAttr.class);
        derAttr.setOwner(owner);
        derAttr.setSchema(sderived);

        derAttr = derAttrDAO.save(derAttr);
        derAttrDAO.flush();

        derAttr = derAttrDAO.find(derAttr.getKey(), GDerAttr.class);
        assertNotNull("expected save to work", derAttr);

        String value = derAttr.getValue(owner.getPlainAttrs());
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.startsWith("managingDirector"));
    }
}
