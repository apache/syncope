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
package org.syncope.core.persistence.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.UDerAttr;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.dao.DerivedAttributeDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.AbstractTest;

@Transactional
public class DerivedAttributeTest extends AbstractTest {

    @Autowired
    private DerivedAttributeDAO derivedAttributeDAO;
    @Autowired
    private SyncopeUserDAO syncopeUserDAO;
    @Autowired
    private DerivedSchemaDAO derivedSchemaDAO;

    @Test
    public final void findAll() {
        List<UDerAttr> list = derivedAttributeDAO.findAll(
                UDerAttr.class);
        assertEquals("did not get expected number of derived attributes ",
                1, list.size());
    }

    @Test
    public final void findById() {
        UDerAttr attribute = derivedAttributeDAO.find(1000L,
                UDerAttr.class);
        assertNotNull("did not find expected attribute schema",
                attribute);
    }

    @Test
    public final void save() throws ClassNotFoundException {
        UDerSchema cnSchema =
                derivedSchemaDAO.find("cn", UDerSchema.class);
        assertNotNull(cnSchema);

        SyncopeUser owner = syncopeUserDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derivedAttribute = new UDerAttr();
        derivedAttribute.setOwner(owner);
        derivedAttribute.setDerivedSchema(cnSchema);

        derivedAttribute = derivedAttributeDAO.save(derivedAttribute);

        UDerAttr actual = derivedAttributeDAO.find(
                derivedAttribute.getId(), UDerAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttribute, actual);

        UAttrValue firstnameAttribute =
                (UAttrValue) owner.getAttribute(
                "firstname").getValues().iterator().next();
        UAttrValue surnameAttribute =
                (UAttrValue) owner.getAttribute(
                "surname").getValues().iterator().next();
        
        assertEquals(surnameAttribute.getValue() + ", "
                + firstnameAttribute.getValue(),
                derivedAttribute.getValue(owner.getAttributes()));
    }

    @Test
    public final void delete() {
        UDerAttr attribute = derivedAttributeDAO.find(1000L,
                UDerAttr.class);
        String attributeSchemaName =
                attribute.getDerivedSchema().getName();

        derivedAttributeDAO.delete(attribute.getId(),
                UDerAttr.class);

        UDerAttr actual = derivedAttributeDAO.find(1000L,
                UDerAttr.class);
        assertNull("delete did not work", actual);

        UDerSchema attributeSchema =
                derivedSchemaDAO.find(attributeSchemaName,
                UDerSchema.class);
        assertNotNull("user derived attribute schema deleted "
                + "when deleting values",
                attributeSchema);
    }
}
