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
package org.apache.syncope.core.persistence.relationships;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttr;
import org.apache.syncope.core.persistence.beans.user.UAttrValue;
import org.apache.syncope.core.persistence.beans.user.UDerAttr;
import org.apache.syncope.core.persistence.beans.user.UDerSchema;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.persistence.dao.AbstractDAOTest;
import org.apache.syncope.core.persistence.dao.AttrDAO;
import org.apache.syncope.core.persistence.dao.AttrValueDAO;
import org.apache.syncope.core.persistence.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class UserTest extends AbstractDAOTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private AttrValueDAO attrValueDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Test
    public void test() {
        userDAO.delete(4L);

        userDAO.flush();

        assertNull(userDAO.find(4L));
        assertNull(attrDAO.find(550L, UAttr.class));
        assertNull(attrValueDAO.find(22L, UAttrValue.class));
        assertNotNull(schemaDAO.find("loginDate", USchema.class));

        List<Membership> memberships = roleDAO.findMemberships(roleDAO.find(7L));
        assertTrue(memberships.isEmpty());
    }

    @Test // search by derived attribute
    public void issueSYNCOPE800() {
        // create derived attribute (literal as prefix)
        UDerSchema prefix = new UDerSchema();
        prefix.setName("kprefix");
        prefix.setExpression("'k' + firstname");

        prefix = derSchemaDAO.save(prefix);
        derSchemaDAO.flush();

        // create derived attribute (literal as suffix)
        UDerSchema suffix = new UDerSchema();
        suffix.setName("ksuffix");
        suffix.setExpression("firstname + 'k'");

        suffix = derSchemaDAO.save(suffix);
        derSchemaDAO.flush();

        // add derived attributes to user
        SyncopeUser owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derivedAttribute = new UDerAttr();
        derivedAttribute.setOwner(owner);
        derivedAttribute.setSchema(prefix);

        derAttrDAO.save(derivedAttribute);
        derAttrDAO.flush();

        derivedAttribute = new UDerAttr();
        derivedAttribute.setOwner(owner);
        derivedAttribute.setSchema(suffix);

        derAttrDAO.save(derivedAttribute);
        derAttrDAO.flush();

        String firstname = owner.getAttr("firstname").getValuesAsStrings().iterator().next();

        // search by ksuffix derived attribute
        List<SyncopeUser> list = userDAO.findByDerAttrValue("ksuffix", firstname + "k");
        assertEquals("did not get expected number of users ", 1, list.size());

        // search by kprefix derived attribute
        list = userDAO.findByDerAttrValue("kprefix", "k" + firstname);
        assertEquals("did not get expected number of users ", 1, list.size());
    }
}
