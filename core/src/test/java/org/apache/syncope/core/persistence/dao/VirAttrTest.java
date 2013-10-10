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
package org.apache.syncope.core.persistence.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.apache.syncope.core.persistence.beans.membership.MVirAttr;
import org.apache.syncope.core.persistence.beans.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.RVirAttr;
import org.apache.syncope.core.persistence.beans.role.RVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UVirAttr;
import org.apache.syncope.core.persistence.beans.user.UVirSchema;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class VirAttrTest extends AbstractDAOTest {

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
    public void findAll() {
        List<UVirAttr> list = virAttrDAO.findAll(UVirAttr.class);
        assertEquals("did not get expected number of derived attributes ", 1, list.size());
    }

    @Test
    public void findById() {
        UVirAttr attribute = virAttrDAO.find(1000L, UVirAttr.class);
        assertNotNull("did not find expected attribute schema", attribute);
    }

    @Test
    public void saveUVirAttribute() {
        UVirSchema virSchema = virSchemaDAO.find("virtualdata", UVirSchema.class);
        assertNotNull(virSchema);

        SyncopeUser owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UVirAttr virAttr = new UVirAttr();
        virAttr.setOwner(owner);
        virAttr.setSchema(virSchema);

        virAttr = virAttrDAO.save(virAttr);

        UVirAttr actual = virAttrDAO.find(virAttr.getId(), UVirAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(virAttr, actual);
    }

    @Test
    public void saveMVirAttribute() {
        Membership owner = membershipDAO.find(3L);
        assertNotNull("did not get expected membership", owner);

        MVirAttr virAttr = new MVirAttr();
        virAttr.setOwner(owner);
        virAttr.setTemplate(owner.getSyncopeRole().getAttrTemplate(MVirAttrTemplate.class, "mvirtualdata"));

        virAttr = virAttrDAO.save(virAttr);
        assertNotNull(virAttr.getTemplate());

        MVirAttr actual = virAttrDAO.find(virAttr.getId(), MVirAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(virAttr, actual);
    }

    @Test
    public void saveRVirAttribute() {
        SyncopeRole owner = roleDAO.find(3L);
        assertNotNull("did not get expected membership", owner);

        RVirAttr virAttr = new RVirAttr();
        virAttr.setOwner(owner);
        virAttr.setTemplate(owner.getAttrTemplate(RVirAttrTemplate.class, "rvirtualdata"));

        virAttr = virAttrDAO.save(virAttr);
        assertNotNull(virAttr.getTemplate());

        RVirAttr actual = virAttrDAO.find(virAttr.getId(), RVirAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(virAttr, actual);
    }

    @Test
    public void delete() {
        UVirAttr attribute = virAttrDAO.find(1000L, UVirAttr.class);
        String attributeSchemaName = attribute.getSchema().getName();

        virAttrDAO.delete(attribute.getId(), UVirAttr.class);

        UVirAttr actual = virAttrDAO.find(1000L, UVirAttr.class);
        assertNull("delete did not work", actual);

        UVirSchema attributeSchema = virSchemaDAO.find(attributeSchemaName, UVirSchema.class);

        assertNotNull("user virtual attribute schema deleted " + "when deleting values", attributeSchema);
    }
}
