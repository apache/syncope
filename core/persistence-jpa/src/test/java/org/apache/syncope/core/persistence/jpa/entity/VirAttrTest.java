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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.MembershipDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttr;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UVirAttr;
import org.apache.syncope.core.persistence.api.entity.user.UVirSchema;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class VirAttrTest extends AbstractTest {

    @Autowired
    private VirAttrDAO virAttrDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

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

        User owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UVirAttr virAttr = entityFactory.newEntity(UVirAttr.class);
        virAttr.setOwner(owner);
        virAttr.setSchema(virSchema);

        virAttr = virAttrDAO.save(virAttr);

        UVirAttr actual = virAttrDAO.find(virAttr.getKey(), UVirAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(virAttr, actual);
    }

    @Test
    public void saveMVirAttribute() {
        Membership owner = membershipDAO.find(3L);
        assertNotNull("did not get expected membership", owner);

        MVirAttr virAttr = entityFactory.newEntity(MVirAttr.class);
        virAttr.setOwner(owner);
        virAttr.setTemplate(owner.getGroup().getAttrTemplate(MVirAttrTemplate.class, "mvirtualdata"));

        virAttr = virAttrDAO.save(virAttr);
        assertNotNull(virAttr.getTemplate());

        MVirAttr actual = virAttrDAO.find(virAttr.getKey(), MVirAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(virAttr, actual);
    }

    @Test
    public void saveRVirAttribute() {
        Group owner = groupDAO.find(3L);
        assertNotNull("did not get expected membership", owner);

        GVirAttr virAttr = entityFactory.newEntity(GVirAttr.class);
        virAttr.setOwner(owner);
        virAttr.setTemplate(owner.getAttrTemplate(GVirAttrTemplate.class, "rvirtualdata"));

        virAttr = virAttrDAO.save(virAttr);
        assertNotNull(virAttr.getTemplate());

        GVirAttr actual = virAttrDAO.find(virAttr.getKey(), GVirAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(virAttr, actual);
    }

    @Test
    public void delete() {
        UVirAttr attribute = virAttrDAO.find(1000L, UVirAttr.class);
        String attributeSchemaName = attribute.getSchema().getKey();

        virAttrDAO.delete(attribute.getKey(), UVirAttr.class);

        UVirAttr actual = virAttrDAO.find(1000L, UVirAttr.class);
        assertNull("delete did not work", actual);

        UVirSchema attributeSchema = virSchemaDAO.find(attributeSchemaName, UVirSchema.class);

        assertNotNull("user virtual attribute schema deleted " + "when deleting values", attributeSchema);
    }
}
