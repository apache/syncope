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
package org.apache.syncope.core.persistence.relationships;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.core.persistence.beans.membership.MAttr;
import org.apache.syncope.core.persistence.beans.membership.MAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MSchema;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.RAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttr;
import org.apache.syncope.core.persistence.beans.user.UAttrValue;
import org.apache.syncope.core.persistence.beans.user.UDerAttr;
import org.apache.syncope.core.persistence.beans.user.UDerSchema;
import org.apache.syncope.core.persistence.dao.AbstractDAOTest;
import org.apache.syncope.core.persistence.dao.AttrDAO;
import org.apache.syncope.core.persistence.dao.AttrValueDAO;
import org.apache.syncope.core.persistence.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.dao.MembershipDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AttrTest extends AbstractDAOTest {

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Autowired
    private AttrValueDAO attrValueDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private MembershipDAO membershipDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void deleteAttribute() {
        attrDAO.delete(117L, UAttr.class);

        attrDAO.flush();

        assertNull(attrDAO.find(117L, UAttr.class));
        assertNull(attrValueDAO.find(28L, UAttrValue.class));
    }

    @Test
    public void deleteAttributeValue() {
        UAttrValue value = attrValueDAO.find(14L, UAttrValue.class);
        int attributeValueNumber = value.getAttribute().getValues().size();

        attrValueDAO.delete(value.getId(), UAttrValue.class);

        attrValueDAO.flush();

        assertNull(attrValueDAO.find(value.getId(), UAttrValue.class));

        UAttr attribute = attrDAO.find(104L, UAttr.class);
        assertEquals(attribute.getValues().size(), attributeValueNumber - 1);
    }

    @Test
    public void checkForEnumType() {
        SyncopeUser user = userDAO.find(1L);
        Membership membership = user.getMembership(1L);
        assertNotNull(membership);

        MSchema schema = new MSchema();
        schema.setType(AttributeSchemaType.Enum);
        schema.setName("color");
        schema.setEnumerationValues("red" + SyncopeConstants.ENUM_VALUES_SEPARATOR + "yellow");

        MSchema actualSchema = schemaDAO.save(schema);
        assertNotNull(actualSchema);

        MAttrTemplate template = new MAttrTemplate();
        template.setSchema(actualSchema);
        membership.getSyncopeRole().getAttrTemplates(MAttrTemplate.class).add(template);

        MAttr attr = new MAttr();
        attr.setTemplate(template);
        attr.setOwner(membership);
        attr.addValue("yellow", AttributableUtil.getInstance(AttributableType.MEMBERSHIP));
        membership.addAttr(attr);

        MAttr actualAttribute = userDAO.save(user).getMembership(1L).getAttr("color");
        assertNotNull(actualAttribute);

        membership = membershipDAO.find(1L);
        assertNotNull(membership);
        assertNotNull(membership.getAttr(schema.getName()));
        assertNotNull(membership.getAttr(schema.getName()).getValues());

        assertEquals(membership.getAttr(schema.getName()).getValues().size(), 1);
    }

    @Test
    public void derAttrFromSpecialAttrs() {
        UDerSchema sderived = new UDerSchema();
        sderived.setName("sderived");
        sderived.setExpression("username + ' - ' + creationDate + '[' + failedLogins + ']'");

        sderived = derSchemaDAO.save(sderived);
        derSchemaDAO.flush();

        UDerSchema actual = derSchemaDAO.find("sderived", UDerSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(sderived, actual);

        SyncopeUser owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derAttr = new UDerAttr();
        derAttr.setOwner(owner);
        derAttr.setSchema(sderived);

        derAttr = derAttrDAO.save(derAttr);
        derAttrDAO.flush();

        derAttr = derAttrDAO.find(derAttr.getId(), UDerAttr.class);
        assertNotNull("expected save to work", derAttr);

        String value = derAttr.getValue(owner.getAttrs());
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.startsWith("vivaldi - 2010-10-20"));
        assertTrue(value.endsWith("[0]"));
    }

    @Test
    public void unmatchedRoleAttr() {
        SyncopeRole role = roleDAO.find(1L);
        assertNotNull(role);

        assertNotNull(role.getAttrTemplate(RAttrTemplate.class, "icon"));
        assertNotNull(role.getAttr("icon"));

        assertTrue(role.getAttrTemplates(RAttrTemplate.class).
                remove(role.getAttrTemplate(RAttrTemplate.class, "icon")));

        role = roleDAO.save(role);
        roleDAO.flush();

        assertNull(role.getAttrTemplate(RAttrTemplate.class, "icon"));
        assertNull(role.getAttr("icon"));
    }
}
