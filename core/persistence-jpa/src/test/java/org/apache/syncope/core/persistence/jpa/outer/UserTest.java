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
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
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

        userDAO.delete("c9b2dec2-00a7-4855-97c0-d854842b4b24");

        userDAO.flush();

        assertNull(userDAO.findByUsername("bellini"));
        assertNull(plainAttrDAO.find(UUID.randomUUID().toString(), UPlainAttr.class));
        assertNull(plainAttrValueDAO.find(UUID.randomUUID().toString(), UPlainAttrValue.class));
        assertNotNull(plainSchemaDAO.find("loginDate"));

        memberships = groupDAO.findUMemberships(groupDAO.findByName("managingDirector"));
        assertTrue(memberships.isEmpty());
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

    @Test
    public void membershipWithAttrs() {
        User user = userDAO.findByUsername("vivaldi");
        assertNotNull(user);
        assertTrue(user.getMemberships().isEmpty());

        // add 'obscure' to user (no membership): works because 'obscure' is from 'other', default class for USER
        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(plainSchemaDAO.find("obscure"));
        attr.add("testvalue", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        // add 'obscure' to user (via 'artDirector' membership): does not work because 'obscure' is from 'other'
        // but 'artDirector' defines no type extension
        UMembership membership = entityFactory.newEntity(UMembership.class);
        membership.setLeftEnd(user);
        membership.setRightEnd(groupDAO.findByName("artDirector"));
        user.add(membership);

        attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setMembership(membership);
        attr.setSchema(plainSchemaDAO.find("obscure"));
        attr.add("testvalue2", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        try {
            userDAO.save(user);
            fail();
        } catch (InvalidEntityException e) {
            assertNotNull(e);
        }

        // replace 'artDirector' with 'additional', which defines type extension with class 'other' and 'csv':
        // now it works
        membership = user.getMembership(groupDAO.findByName("artDirector").getKey()).get();
        user.remove(user.getPlainAttr("obscure", membership).get());
        user.getMemberships().remove(membership);
        membership.setLeftEnd(null);

        membership = entityFactory.newEntity(UMembership.class);
        membership.setLeftEnd(user);
        membership.setRightEnd(groupDAO.findByName("additional"));
        user.add(membership);

        attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setMembership(membership);
        attr.setSchema(plainSchemaDAO.find("obscure"));
        attr.add("testvalue2", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        userDAO.save(user);
        userDAO.flush();

        user = userDAO.findByUsername("vivaldi");
        assertEquals(1, user.getMemberships().size());

        final UMembership newM = user.getMembership(groupDAO.findByName("additional").getKey()).get();
        assertEquals(1, user.getPlainAttrs(newM).size());

        assertNull(user.getPlainAttr("obscure").get().getMembership());
        assertEquals(2, user.getPlainAttrs("obscure").size());
        assertTrue(user.getPlainAttrs("obscure").contains(user.getPlainAttr("obscure").get()));
        assertTrue(user.getPlainAttrs("obscure").stream().anyMatch(plainAttr -> plainAttr.getMembership() == null));
        assertTrue(user.getPlainAttrs("obscure").stream().anyMatch(plainAttr -> newM.equals(plainAttr.getMembership())));
    }

    /**
     * Search by derived attribute.
     */
    @Test
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

        String firstname = owner.getPlainAttr("firstname").get().getValuesAsStrings().iterator().next();
        assertNotNull(firstname);

        // search by ksuffix derived attribute
        List<User> list = userDAO.findByDerAttrValue("ksuffix", firstname + "k");
        assertEquals("did not get expected number of users ", 1, list.size());

        // search by kprefix derived attribute
        list = userDAO.findByDerAttrValue("kprefix", "k" + firstname);
        assertEquals("did not get expected number of users ", 1, list.size());
    }

    @Test
    public void issueSYNCOPE1016() {
        User user = userDAO.findByUsername("rossini");
        Date initial = user.getLastChangeDate();
        assertNotNull(initial);

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(plainSchemaDAO.find("obscure"));
        attr.add("testvalue", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        userDAO.save(user);

        userDAO.flush();

        user = userDAO.findByUsername("rossini");
        Date afterwards = user.getLastChangeDate();
        assertNotNull(afterwards);

        assertTrue(afterwards.after(initial));
    }
}
