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
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.membership.MAttrValue;
import org.syncope.core.persistence.beans.membership.MDerAttr;
import org.syncope.core.persistence.beans.membership.MDerSchema;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.RAttrValue;
import org.syncope.core.persistence.beans.role.RDerAttr;
import org.syncope.core.persistence.beans.role.RDerSchema;
import org.syncope.core.persistence.beans.role.SyncopeRole;

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
    public final void findAll() {
        List<UDerAttr> list = derAttrDAO.findAll(
                UDerAttr.class);
        assertEquals("did not get expected number of derived attributes ",
                1, list.size());
    }

    @Test
    public final void findById() {
        UDerAttr attribute = derAttrDAO.find(1000L,
                UDerAttr.class);
        assertNotNull("did not find expected attribute schema",
                attribute);
    }

    @Test
    public final void saveUDerAttribute()
            throws ClassNotFoundException {
        UDerSchema cnSchema =
                derSchemaDAO.find("cn", UDerSchema.class);
        assertNotNull(cnSchema);

        SyncopeUser owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derivedAttribute = new UDerAttr();
        derivedAttribute.setOwner(owner);
        derivedAttribute.setDerivedSchema(cnSchema);

        derivedAttribute = derAttrDAO.save(derivedAttribute);

        UDerAttr actual = derAttrDAO.find(
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
    public final void saveMDerAttribute()
            throws ClassNotFoundException {
        MDerSchema deriveddata =
                derSchemaDAO.find("deriveddata", MDerSchema.class);
        assertNotNull(deriveddata);

        Membership owner = membershipDAO.find(1L);
        assertNotNull("did not get expected user", owner);

        MDerAttr derivedAttribute = new MDerAttr();
        derivedAttribute.setOwner(owner);
        derivedAttribute.setDerivedSchema(deriveddata);

        derivedAttribute = derAttrDAO.save(derivedAttribute);

        MDerAttr actual = derAttrDAO.find(
                derivedAttribute.getId(), MDerAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttribute, actual);

        MAttrValue sx =
                (MAttrValue) owner.getAttribute(
                "derived_sx").getValues().iterator().next();
        MAttrValue dx =
                (MAttrValue) owner.getAttribute(
                "derived_dx").getValues().iterator().next();

        assertEquals(sx.getValue() + "-" + dx.getValue(),
                derivedAttribute.getValue(owner.getAttributes()));
    }

    @Test
    public final void saveRDerAttribute()
            throws ClassNotFoundException {
        RDerSchema deriveddata =
                derSchemaDAO.find("deriveddata", RDerSchema.class);
        assertNotNull(deriveddata);

        SyncopeRole owner = roleDAO.find(1L);
        assertNotNull("did not get expected user", owner);

        RDerAttr derivedAttribute = new RDerAttr();
        derivedAttribute.setOwner(owner);
        derivedAttribute.setDerivedSchema(deriveddata);

        derivedAttribute = derAttrDAO.save(derivedAttribute);

        RDerAttr actual = derAttrDAO.find(
                derivedAttribute.getId(), RDerAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttribute, actual);

        RAttrValue sx =
                (RAttrValue) owner.getAttribute(
                "derived_sx").getValues().iterator().next();
        RAttrValue dx =
                (RAttrValue) owner.getAttribute(
                "derived_dx").getValues().iterator().next();

        assertEquals(sx.getValue() + "-" + dx.getValue(),
                derivedAttribute.getValue(owner.getAttributes()));
    }

    @Test
    public final void delete() {
        UDerAttr attribute = derAttrDAO.find(1000L,
                UDerAttr.class);
        String attributeSchemaName =
                attribute.getDerivedSchema().getName();

        derAttrDAO.delete(attribute.getId(),
                UDerAttr.class);

        UDerAttr actual = derAttrDAO.find(1000L,
                UDerAttr.class);
        assertNull("delete did not work", actual);

        UDerSchema attributeSchema =
                derSchemaDAO.find(attributeSchemaName,
                UDerSchema.class);
        assertNotNull("user derived attribute schema deleted "
                + "when deleting values",
                attributeSchema);
    }
}
