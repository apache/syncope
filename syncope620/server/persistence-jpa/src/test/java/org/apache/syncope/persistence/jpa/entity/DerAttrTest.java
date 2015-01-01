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
package org.apache.syncope.persistence.jpa.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.syncope.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.persistence.api.dao.MembershipDAO;
import org.apache.syncope.persistence.api.dao.RoleDAO;
import org.apache.syncope.persistence.api.dao.UserDAO;
import org.apache.syncope.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.persistence.api.entity.membership.MPlainAttrValue;
import org.apache.syncope.persistence.api.entity.membership.Membership;
import org.apache.syncope.persistence.api.entity.role.RDerAttr;
import org.apache.syncope.persistence.api.entity.role.RDerAttrTemplate;
import org.apache.syncope.persistence.api.entity.role.RDerSchema;
import org.apache.syncope.persistence.api.entity.role.RPlainAttrValue;
import org.apache.syncope.persistence.api.entity.role.Role;
import org.apache.syncope.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.persistence.api.entity.user.UDerSchema;
import org.apache.syncope.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.persistence.api.entity.user.User;
import org.apache.syncope.persistence.jpa.AbstractTest;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMDerAttr;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMDerAttrTemplate;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMDerSchema;
import org.apache.syncope.persistence.jpa.entity.role.JPARDerAttr;
import org.apache.syncope.persistence.jpa.entity.role.JPARDerAttrTemplate;
import org.apache.syncope.persistence.jpa.entity.role.JPARDerSchema;
import org.apache.syncope.persistence.jpa.entity.user.JPAUDerAttr;
import org.apache.syncope.persistence.jpa.entity.user.JPAUDerSchema;
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
    private MembershipDAO membershipDAO;

    @Autowired
    private RoleDAO roleDAO;

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
        UDerSchema cnSchema = derSchemaDAO.find("cn", UDerSchema.class);
        assertNotNull(cnSchema);

        User owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derAttr = new JPAUDerAttr();
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
    public void saveMDerAttribute() {
        Membership owner = membershipDAO.find(1L);
        assertNotNull("did not get expected user", owner);

        MDerAttr derAttr = new JPAMDerAttr();
        derAttr.setOwner(owner);
        derAttr.setTemplate(owner.getRole().getAttrTemplate(MDerAttrTemplate.class, "mderiveddata"));

        derAttr = derAttrDAO.save(derAttr);
        assertNotNull(derAttr.getTemplate());

        MDerAttr actual = derAttrDAO.find(derAttr.getKey(), MDerAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derAttr, actual);

        MPlainAttrValue sx = owner.getPlainAttr("mderived_sx").getValues().iterator().next();
        MPlainAttrValue dx = owner.getPlainAttr("mderived_dx").getValues().iterator().next();

        assertEquals(sx.getValue() + "-" + dx.getValue(), derAttr.getValue(owner.getPlainAttrs()));
    }

    @Test
    public void saveRDerAttribute() {
        Role owner = roleDAO.find(1L);
        assertNotNull("did not get expected user", owner);

        RDerAttr derAttr = new JPARDerAttr();
        derAttr.setOwner(owner);
        derAttr.setTemplate(owner.getAttrTemplate(RDerAttrTemplate.class, "rderiveddata"));

        derAttr = derAttrDAO.save(derAttr);
        assertNotNull(derAttr.getTemplate());

        RDerAttr actual = derAttrDAO.find(derAttr.getKey(), RDerAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derAttr, actual);

        RPlainAttrValue sx = owner.getPlainAttr("rderived_sx").getValues().iterator().next();
        RPlainAttrValue dx = owner.getPlainAttr("rderived_dx").getValues().iterator().next();

        assertEquals(sx.getValue() + "-" + dx.getValue(), derAttr.getValue(owner.getPlainAttrs()));
    }

    @Test
    public void delete() {
        UDerAttr attribute = derAttrDAO.find(100L, UDerAttr.class);
        String attributeSchemaName = attribute.getSchema().getKey();

        derAttrDAO.delete(attribute.getKey(), UDerAttr.class);

        UDerAttr actual = derAttrDAO.find(100L, UDerAttr.class);
        assertNull("delete did not work", actual);

        UDerSchema attributeSchema = derSchemaDAO.find(attributeSchemaName, UDerSchema.class);
        assertNotNull("user derived attribute schema deleted " + "when deleting values", attributeSchema);
    }

    @Test
    public void issueSYNCOPE134User() {
        UDerSchema sderived = new JPAUDerSchema();
        sderived.setKey("sderived");
        sderived.setExpression("status + ' - ' + username + ' - ' + creationDate + '[' + failedLogins + ']'");

        sderived = derSchemaDAO.save(sderived);
        derSchemaDAO.flush();

        UDerSchema actual = derSchemaDAO.find("sderived", UDerSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(sderived, actual);

        User owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derAttr = new JPAUDerAttr();
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
    public void issueSYNCOPE134Role() {
        RDerSchema sderived = new JPARDerSchema();
        sderived.setKey("sderived");
        sderived.setExpression("name");

        sderived = derSchemaDAO.save(sderived);
        derSchemaDAO.flush();

        RDerSchema actual = derSchemaDAO.find("sderived", RDerSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(sderived, actual);

        Role owner = roleDAO.find(7L);
        assertNotNull("did not get expected role", owner);

        RDerAttrTemplate template = new JPARDerAttrTemplate();
        template.setSchema(sderived);
        owner.getAttrTemplates(RDerAttrTemplate.class).add(template);

        RDerAttr derAttr = new JPARDerAttr();
        derAttr.setOwner(owner);
        derAttr.setTemplate(owner.getAttrTemplate(RDerAttrTemplate.class, sderived.getKey()));

        derAttr = derAttrDAO.save(derAttr);
        assertNotNull(derAttr.getTemplate());
        derAttrDAO.flush();

        derAttr = derAttrDAO.find(derAttr.getKey(), RDerAttr.class);
        assertNotNull("expected save to work", derAttr);

        String value = derAttr.getValue(owner.getPlainAttrs());
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.startsWith("managingDirector"));
    }

    @Test
    public void issueSYNCOPE134Memb() {
        MDerSchema mderived = new JPAMDerSchema();
        mderived.setKey("mderived");
        mderived.setExpression("key");

        mderived = derSchemaDAO.save(mderived);
        derSchemaDAO.flush();

        MDerSchema actual = derSchemaDAO.find("mderived", MDerSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(mderived, actual);

        Membership owner = membershipDAO.find(4L);
        assertNotNull("did not get expected membership", owner);

        MDerAttrTemplate template = new JPAMDerAttrTemplate();
        template.setSchema(mderived);
        owner.getRole().getAttrTemplates(MDerAttrTemplate.class).add(template);

        derSchemaDAO.flush();

        MDerAttr derAttr = new JPAMDerAttr();
        derAttr.setOwner(owner);
        derAttr.setTemplate(owner.getRole().getAttrTemplate(MDerAttrTemplate.class, mderived.getKey()));

        derAttr = derAttrDAO.save(derAttr);
        assertNotNull(derAttr.getTemplate());
        derAttrDAO.flush();

        derAttr = derAttrDAO.find(derAttr.getKey(), MDerAttr.class);
        assertNotNull("expected save to work", derAttr);

        String value = derAttr.getValue(owner.getPlainAttrs());
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.equalsIgnoreCase("4"));
    }
}
