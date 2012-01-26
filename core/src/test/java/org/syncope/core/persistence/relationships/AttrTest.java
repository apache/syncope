/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.relationships;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.dao.AttrDAO;
import org.syncope.core.persistence.dao.AttrValueDAO;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.membership.MAttr;
import org.syncope.core.persistence.beans.membership.MSchema;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UDerAttr;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.dao.DerAttrDAO;
import org.syncope.core.persistence.dao.DerSchemaDAO;
import org.syncope.core.persistence.dao.MembershipDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.util.AttributableUtil;
import org.syncope.types.SchemaType;

@Transactional
public class AttrTest extends AbstractTest {

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
    private UserDAO userDAO;

    @Test
    public void deleteAttribute() {
        attrDAO.delete(550L, UAttr.class);

        attrDAO.flush();

        assertNull(attrDAO.find(550L, UAttr.class));
        assertNull(attrValueDAO.find(22L, UAttrValue.class));
    }

    @Test
    public void deleteAttributeValue() {
        UAttrValue value =
                attrValueDAO.find(20L, UAttrValue.class);
        int attributeValueNumber =
                value.getAttribute().getValues().size();

        attrValueDAO.delete(20L, UAttrValue.class);

        attrValueDAO.flush();

        assertNull(attrValueDAO.find(20L, UAttrValue.class));

        UAttr attribute = attrDAO.find(200L, UAttr.class);
        assertEquals(attribute.getValues().size(),
                attributeValueNumber - 1);
    }

    @Test
    public void checkForEnumType() {
        MSchema schema = new MSchema();
        schema.setType(SchemaType.Enum);
        schema.setName("color");
        schema.setEnumerationValues(
                "red" + AbstractSchema.enumValuesSeparator + "yellow");

        MSchema actualSchema = schemaDAO.save(schema);
        assertNotNull(actualSchema);

        Membership membership = membershipDAO.find(1L);
        assertNotNull(membership);

        MAttr attribute = new MAttr();
        attribute.setSchema(actualSchema);
        attribute.setOwner(membership);
        attribute.addValue("yellow", AttributableUtil.MEMBERSHIP);
        membership.addAttribute(attribute);

        MAttr actualAttribute = attrDAO.save(attribute);
        assertNotNull(actualAttribute);

        membership = membershipDAO.find(1L);
        assertNotNull(membership);
        assertNotNull(membership.getAttribute(schema.getName()));
        assertNotNull(membership.getAttribute(schema.getName()).getValues());

        assertEquals(
                membership.getAttribute(schema.getName()).getValues().size(),
                1);
    }

    public void derAttrFromSpecialAttrs() {
        UDerSchema sderived = new UDerSchema();
        sderived.setName("sderived");
        sderived.setExpression("username - creationDate[failedLogins]");

        derSchemaDAO.save(sderived);
        derSchemaDAO.flush();

        UDerSchema actual = derSchemaDAO.find("sderived", UDerSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(sderived, actual);

        SyncopeUser owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derAttr = new UDerAttr();
        derAttr.setOwner(owner);
        derAttr.setDerivedSchema(sderived);

        derAttr = derAttrDAO.save(derAttr);
        derAttrDAO.flush();

        derAttr = derAttrDAO.find(derAttr.getId(), UDerAttr.class);
        assertNotNull("expected save to work", derAttr);

        String value = derAttr.getValue(owner.getAttributes());
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.startsWith("user3 - 2010-10-20T11:00:00"));
        assertTrue(value.endsWith("[]"));
    }
}
