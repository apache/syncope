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
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.membership.MVirAttr;
import org.syncope.core.persistence.beans.membership.MVirSchema;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.RVirAttr;
import org.syncope.core.persistence.beans.role.RVirSchema;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.UVirAttr;
import org.syncope.core.persistence.beans.user.UVirSchema;

@Transactional
public class VirAttrTest extends AbstractTest {

    @Autowired
    private VirAttrDAO virAttrDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private MembershipDAO membershipDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Test
    public final void findAll() {
        List<UVirAttr> list = virAttrDAO.findAll(UVirAttr.class);
        assertEquals(
                "did not get expected number of derived attributes ",
                1, list.size());
    }

    @Test
    public final void findById() {
        UVirAttr attribute = virAttrDAO.find(100L, UVirAttr.class);
        assertNotNull(
                "did not find expected attribute schema",
                attribute);
    }

    @Test
    public final void saveUVirAttribute()
            throws ClassNotFoundException {
        UVirSchema virtualSchema =
                virSchemaDAO.find("virtualdata", UVirSchema.class);
        assertNotNull(virtualSchema);

        SyncopeUser owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UVirAttr virtualAttribute = new UVirAttr();
        virtualAttribute.setOwner(owner);
        virtualAttribute.setVirtualSchema(virtualSchema);

        virtualAttribute = virAttrDAO.save(virtualAttribute);

        UVirAttr actual = virAttrDAO.find(
                virtualAttribute.getId(), UVirAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(virtualAttribute, actual);
    }

    @Test
    public final void saveMVirAttribute()
            throws ClassNotFoundException {

        MVirSchema virtualSchema = new MVirSchema();
        virtualSchema.setName("mvirtualdata");

        Membership owner = membershipDAO.find(3L);
        assertNotNull("did not get expected membership", owner);

        MVirAttr virtualAttribute = new MVirAttr();
        virtualAttribute.setOwner(owner);
        virtualAttribute.setVirtualSchema(virtualSchema);

        virtualAttribute = virAttrDAO.save(virtualAttribute);

        MVirAttr actual = virAttrDAO.find(
                virtualAttribute.getId(), MVirAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(virtualAttribute, actual);
    }

    @Test
    public final void saveRVirAttribute()
            throws ClassNotFoundException {

        RVirSchema virtualSchema = new RVirSchema();
        virtualSchema.setName("rvirtualdata");

        SyncopeRole owner = roleDAO.find(3L);
        assertNotNull("did not get expected membership", owner);

        RVirAttr virtualAttribute = new RVirAttr();
        virtualAttribute.setOwner(owner);
        virtualAttribute.setVirtualSchema(virtualSchema);

        virtualAttribute = virAttrDAO.save(virtualAttribute);

        RVirAttr actual = virAttrDAO.find(
                virtualAttribute.getId(), RVirAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(virtualAttribute, actual);
    }

    @Test
    public final void delete() {
        UVirAttr attribute = virAttrDAO.find(100L, UVirAttr.class);
        String attributeSchemaName = attribute.getVirtualSchema().getName();

        virAttrDAO.delete(attribute.getId(), UVirAttr.class);

        UVirAttr actual = virAttrDAO.find(100L, UVirAttr.class);
        assertNull("delete did not work", actual);

        UVirSchema attributeSchema =
                virSchemaDAO.find(attributeSchemaName,
                UVirSchema.class);

        assertNotNull("user virtual attribute schema deleted "
                + "when deleting values",
                attributeSchema);
    }
}
