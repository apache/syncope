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
import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
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

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Test
    public void delete() {
        List<UMembership> memberships = groupDAO.findUMemberships(groupDAO.findByName("managingDirector"));
        assertFalse(memberships.isEmpty());
        List<URelationship> relationships = anyObjectDAO.findURelationships(
                anyObjectDAO.find("fc6dbc3a-6c07-4965-8781-921e7401a4a5"));
        assertFalse(relationships.isEmpty());

        userDAO.delete("c9b2dec2-00a7-4855-97c0-d854842b4b24");

        userDAO.flush();

        assertNull(userDAO.findByUsername("bellini"));
        assertNull(plainAttrDAO.find(UUID.randomUUID().toString(), UPlainAttr.class));
        assertNull(plainAttrValueDAO.find(UUID.randomUUID().toString(), UPlainAttrValue.class));
        assertNotNull(plainSchemaDAO.find("loginDate"));

        memberships = groupDAO.findUMemberships(groupDAO.findByName("managingDirector"));
        assertTrue(memberships.isEmpty());
        relationships = anyObjectDAO.findURelationships(
                anyObjectDAO.find("fc6dbc3a-6c07-4965-8781-921e7401a4a5"));
        assertTrue(relationships.isEmpty());
    }

    @Test
    public void ships() {
        User user = userDAO.findByUsername("bellini");
        assertNotNull(user);
        assertEquals(1, user.getMemberships().size());
        assertEquals(
                "bf825fe1-7320-4a54-bd64-143b5c18ab97",
                user.getMemberships().get(0).getRightEnd().getKey());

        user.getMemberships().remove(0);

        UMembership newM = entityFactory.newEntity(UMembership.class);
        newM.setLeftEnd(user);
        newM.setRightEnd(groupDAO.find("ba9ed509-b1f5-48ab-a334-c8530a6422dc"));
        user.add(newM);

        userDAO.save(user);

        userDAO.flush();

        user = userDAO.findByUsername("bellini");
        assertEquals(1, user.getMemberships().size());
        assertEquals(
                "ba9ed509-b1f5-48ab-a334-c8530a6422dc",
                user.getMemberships().get(0).getRightEnd().getKey());
        assertEquals(1, user.getRelationships().size());
        assertEquals(
                "fc6dbc3a-6c07-4965-8781-921e7401a4a5",
                user.getRelationships().get(0).getRightEnd().getKey());

        user.getRelationships().remove(0);

        URelationship newR = entityFactory.newEntity(URelationship.class);
        newR.setType(relationshipTypeDAO.find("neighborhood"));
        newR.setLeftEnd(user);
        newR.setRightEnd(anyObjectDAO.find("8559d14d-58c2-46eb-a2d4-a7d35161e8f8"));
        user.add(newR);

        userDAO.save(user);

        userDAO.flush();

        user = userDAO.findByUsername("bellini");
        assertEquals(1, user.getRelationships().size());
        assertEquals(
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8",
                user.getRelationships().get(0).getRightEnd().getKey());
    }

    @Test // search by derived attribute
    public void issueSYNCOPE800() {
        // create derived attribute (literal as prefix)
        DerSchema prefix = entityFactory.newEntity(DerSchema.class);
        prefix.setKey("kprefix");
        prefix.setExpression("'k' + firstname");

        derSchemaDAO.save(prefix);
        derSchemaDAO.flush();

        // create derived attribute (literal as suffix)
        DerSchema suffix = entityFactory.newEntity(DerSchema.class);
        suffix.setKey("ksuffix");
        suffix.setExpression("firstname + 'k'");

        derSchemaDAO.save(suffix);
        derSchemaDAO.flush();

        // add derived attributes to user
        User owner = userDAO.findByUsername("vivaldi");
        assertNotNull("did not get expected user", owner);

        String firstname = owner.getPlainAttr("firstname").getValuesAsStrings().iterator().next();
        assertNotNull(firstname);

        // search by ksuffix derived attribute
        List<User> list = userDAO.findByDerAttrValue("ksuffix", firstname + "k");
        assertEquals("did not get expected number of users ", 1, list.size());

        // search by kprefix derived attribute
        list = userDAO.findByDerAttrValue("kprefix", "k" + firstname);
        assertEquals("did not get expected number of users ", 1, list.size());
    }
}
