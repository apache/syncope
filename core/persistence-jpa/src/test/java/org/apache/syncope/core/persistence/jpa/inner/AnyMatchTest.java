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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AnyMatchTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private AnyMatchDAO anyMatcher;

    @Test
    public void byResourceCond() {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertNotNull(user);

        ResourceCond resourceCond = new ResourceCond();
        resourceCond.setResource("resource-testdb2");
        assertTrue(anyMatcher.matches(user, SearchCond.getLeaf(resourceCond)));

        resourceCond.setResource("ws-target-resource-delete");
        assertFalse(anyMatcher.matches(user, SearchCond.getLeaf(resourceCond)));
    }

    @Test
    public void anyObjectMatch() {
        AnyObject anyObject = anyObjectDAO.find("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
        assertNotNull(anyObject);

        RelationshipCond relationshipCond = new RelationshipCond();
        relationshipCond.setAnyObject("Canon MF 8030cn");
        assertTrue(anyMatcher.matches(anyObject, SearchCond.getLeaf(relationshipCond)));

        RelationshipTypeCond relationshipTypeCond = new RelationshipTypeCond();
        relationshipTypeCond.setRelationshipTypeKey("neighborhood");
        assertTrue(anyMatcher.matches(anyObject, SearchCond.getLeaf(relationshipTypeCond)));
    }

    @Test
    public void userMatch() {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertNotNull(user);

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("secretary");
        assertFalse(anyMatcher.matches(user, SearchCond.getLeaf(groupCond)));

        groupCond.setGroup("root");
        assertTrue(anyMatcher.matches(user, SearchCond.getLeaf(groupCond)));

        RoleCond roleCond = new RoleCond();
        roleCond.setRole("Other");
        assertTrue(anyMatcher.matches(user, SearchCond.getLeaf(roleCond)));

        user = userDAO.find("c9b2dec2-00a7-4855-97c0-d854842b4b24");
        assertNotNull(user);

        RelationshipCond relationshipCond = new RelationshipCond();
        relationshipCond.setAnyObject("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
        assertTrue(anyMatcher.matches(user, SearchCond.getLeaf(relationshipCond)));

        RelationshipTypeCond relationshipTypeCond = new RelationshipTypeCond();
        relationshipTypeCond.setRelationshipTypeKey("neighborhood");
        assertTrue(anyMatcher.matches(user, SearchCond.getLeaf(relationshipTypeCond)));
    }

    @Test
    public void groupMatch() {
        Group group = groupDAO.find("37d15e4c-cdc1-460b-a591-8505c8133806");
        assertNotNull(group);

        AnyCond anyCond = new AnyCond();
        anyCond.setSchema("name");
        anyCond.setExpression("root");
        anyCond.setType(AttrCond.Type.EQ);
        assertTrue(anyMatcher.matches(group, SearchCond.getLeaf(anyCond)));

        AttrCond attrCond = new AttrCond();
        attrCond.setSchema("show");
        attrCond.setType(AttrCond.Type.ISNOTNULL);
        assertTrue(anyMatcher.matches(group, SearchCond.getLeaf(attrCond)));
    }
}
