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
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.membership.MAttr;
import org.syncope.core.persistence.beans.membership.MSchema;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.dao.MembershipDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.util.AttributableUtil;
import org.syncope.types.SchemaType;

@Transactional
public class AttrTest extends AbstractTest {

    @Autowired
    private AttrDAO attributeDAO;

    @Autowired
    private AttrValueDAO attributeValueDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private MembershipDAO membershipDAO;

    @Test
    public final void deleteAttribute() {
        attributeDAO.delete(550L, UAttr.class);

        attributeDAO.flush();

        assertNull(attributeDAO.find(550L, UAttr.class));
        assertNull(attributeValueDAO.find(22L, UAttrValue.class));
    }

    @Test
    public final void deleteAttributeValue() {
        UAttrValue value =
                attributeValueDAO.find(20L, UAttrValue.class);
        int attributeValueNumber =
                value.getAttribute().getValues().size();

        attributeValueDAO.delete(20L, UAttrValue.class);

        attributeValueDAO.flush();

        assertNull(attributeValueDAO.find(20L, UAttrValue.class));

        UAttr attribute = attributeDAO.find(200L, UAttr.class);
        assertEquals(attribute.getValues().size(),
                attributeValueNumber - 1);
    }

    @Test
    public final void checkForEnumType() {
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

        MAttr actualAttribute = attributeDAO.save(attribute);
        assertNotNull(actualAttribute);

        membership = membershipDAO.find(1L);
        assertNotNull(membership);
        assertNotNull(membership.getAttribute(schema.getName()));
        assertNotNull(membership.getAttribute(schema.getName()).getValues());

        assertEquals(
                membership.getAttribute(schema.getName()).getValues().size(),
                1);
    }
}
