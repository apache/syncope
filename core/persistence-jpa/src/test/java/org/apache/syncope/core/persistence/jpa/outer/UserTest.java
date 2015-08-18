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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class UserTest extends AbstractTest {

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private PlainAttrValueDAO plainAttrValueDAO;

    @Test
    public void delete() {
        List<UMembership> memberships = groupDAO.findUMemberships(groupDAO.find(7L));
        assertFalse(memberships.isEmpty());
        List<URelationship> relationships = anyObjectDAO.findURelationships(anyObjectDAO.find(1L));
        assertFalse(relationships.isEmpty());

        userDAO.delete(4L);

        userDAO.flush();

        assertNull(userDAO.find(4L));
        assertNull(plainAttrDAO.find(550L, UPlainAttr.class));
        assertNull(plainAttrValueDAO.find(22L, UPlainAttrValue.class));
        assertNotNull(plainSchemaDAO.find("loginDate"));

        memberships = groupDAO.findUMemberships(groupDAO.find(7L));
        assertTrue(memberships.isEmpty());
        relationships = anyObjectDAO.findURelationships(anyObjectDAO.find(1L));
        assertTrue(relationships.isEmpty());
    }

    @Test
    public void ships() {
        User user = userDAO.find(4L);
        assertNotNull(user);
        assertEquals(1, user.getMemberships().size());
        assertEquals(7L, user.getMemberships().get(0).getRightEnd().getKey(), 0);

        user.remove(user.getMemberships().get(0));

        UMembership newM = entityFactory.newEntity(UMembership.class);
        newM.setLeftEnd(user);
        newM.setRightEnd(groupDAO.find(13L));
        user.add(newM);

        userDAO.save(user);

        userDAO.flush();

        user = userDAO.find(4L);
        assertEquals(1, user.getMemberships().size());
        assertEquals(13L, user.getMemberships().get(0).getRightEnd().getKey(), 0);
        assertEquals(1, user.getRelationships().size());
        assertEquals(1L, user.getRelationships().get(0).getRightEnd().getKey(), 0);

        user.remove(user.getRelationships().get(0));

        URelationship newR = entityFactory.newEntity(URelationship.class);
        newR.setType(relationshipTypeDAO.find("neighborhood"));
        newR.setLeftEnd(user);
        newR.setRightEnd(anyObjectDAO.find(2L));
        user.add(newR);

        userDAO.save(user);

        userDAO.flush();

        user = userDAO.find(4L);
        assertEquals(1, user.getRelationships().size());
        assertEquals(2L, user.getRelationships().get(0).getRightEnd().getKey(), 0);
    }
}
