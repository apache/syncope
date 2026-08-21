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
package org.apache.syncope.core.provisioning.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.jexl.JexlContextBuilder;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JexlToolsTest extends AbstractTest {

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private JexlTools jexlTools;

    @Test
    void permissionFields() {
        UserTO userTO = new UserTO();
        userTO.setUsername("username1");
        userTO.setPassword("userPassword1");

        JexlContext ctx = new JexlContextBuilder().fields(userTO).build();

        assertEquals("username1/", jexlTools.evaluateExpression("username + '/' + password", ctx));
    }

    @Test
    void permissionTemplate() {
        UserTO userTO = new UserTO();
        userTO.setUsername("username1");
        userTO.setPassword("userPassword1");
        userTO.getMemberships().add(new MembershipTO.Builder("groupKey").groupName("groupName").build());

        assertEquals(
                "I am username1, my password is ''; my group is groupName",
                jexlTools.evaluateTemplate(
                        "I am ${user.username}, my password is '${user.password}'; "
                        + "my group is ${user.memberships[0].groupName}",
                        new MapContext(Map.of("user", userTO))));

        User user = entityFactory.newEntity(User.class);
        user.setUsername("username1");
        user.setPassword("password1");

        Group group = entityFactory.newEntity(Group.class);
        group.setName("groupName");
        UMembership membership = entityFactory.newEntity(UMembership.class);
        membership.setLeftEnd(user);
        membership.setRightEnd(group);
        user.add(membership);

        User manager = entityFactory.newEntity(User.class);
        manager.setUsername("usernameM1");
        manager.setPassword("passwordM1");
        user.setuManager(manager);

        assertEquals(
                "I am username1, my password is ''; "
                + "my manager is usernameM1, their password is ''; "
                + "my group is groupName",
                jexlTools.evaluateTemplate(
                        "I am ${user.username}, my password is '${user.password}'; "
                        + "my manager is ${user.uManager.username}, their password is '${user.uManager.password}'; "
                        + "my group is ${user.memberships[0].rightEnd.name}",
                        new MapContext(Map.of("user", user))));
    }
}
