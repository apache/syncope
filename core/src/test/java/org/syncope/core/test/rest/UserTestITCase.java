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
package org.syncope.core.test.rest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import static org.junit.Assert.*;

import java.util.Date;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.MembershipMod;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.LeafSearchCondition;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.NodeSearchCondition;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.UserTOs;
import org.syncope.client.to.WorkflowActionsTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.types.SyncopeClientExceptionType;

public class UserTestITCase extends AbstractTestITCase {

    private UserTO getSampleTO(String email) {
        UserTO userTO = new UserTO();
        userTO.setPassword("password");

        AttributeTO usernameTO = new AttributeTO();
        usernameTO.setSchema("username");
        usernameTO.addValue(email);
        userTO.addAttribute(usernameTO);

        AttributeTO surnameTO = new AttributeTO();
        surnameTO.setSchema("surname");
        surnameTO.addValue("Surname");
        userTO.addAttribute(surnameTO);

        AttributeTO userIdTO = new AttributeTO();
        userIdTO.setSchema("userId");
        userIdTO.addValue(email);
        userTO.addAttribute(userIdTO);

        AttributeTO emailTO = new AttributeTO();
        emailTO.setSchema("email");
        emailTO.addValue(email);
        userTO.addAttribute(emailTO);

        AttributeTO loginDateTO = new AttributeTO();
        loginDateTO.setSchema("loginDate");
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        loginDateTO.addValue(sdf.format(new Date()));
        userTO.addAttribute(loginDateTO);

        return userTO;
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public void createWithException() {
        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.addValue("userId@nowhere.org");

        UserTO newUserTO = new UserTO();
        newUserTO.addAttribute(attributeTO);

        restTemplate.postForObject(BASE_URL + "user/create",
                newUserTO, UserTO.class);
    }

    @Test
    public void create() {
        UserTO userTO = getSampleTO("a.b@c.com");

        AttributeTO attrWithInvalidSchemaTO = new AttributeTO();
        attrWithInvalidSchemaTO.setSchema("invalid schema");
        attrWithInvalidSchemaTO.addValue("a value");
        userTO.addAttribute(attrWithInvalidSchemaTO);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRole(8L);
        userTO.addMembership(membershipTO);

        // 1. create user
        UserTO newUserTO = restTemplate.postForObject(
                BASE_URL + "user/create?syncRoles=8",
                userTO, UserTO.class);

        assertNotNull(newUserTO);
        assertFalse(newUserTO.getAttributes().contains(
                attrWithInvalidSchemaTO));

        WorkflowActionsTO workflowActions = restTemplate.getForObject(BASE_URL
                + "user/actions/{userId}", WorkflowActionsTO.class,
                newUserTO.getId());
        assertTrue(workflowActions.getActions().equals(
                Collections.singleton("activate")));

        // 2. activate user
        newUserTO = restTemplate.postForObject(BASE_URL + "user/activate",
                newUserTO, UserTO.class);
        assertEquals("active",
                restTemplate.getForObject(BASE_URL + "user/status/"
                + newUserTO.getId(), String.class));

        // 3. try (and fail) to create another user with same (unique) values
        userTO = getSampleTO("pippo@c.com");
        AttributeTO userIdTO = new AttributeTO();
        userIdTO.setSchema("userId");
        userIdTO.addValue("a.b@c.com");
        userTO.addAttribute(userIdTO);

        SyncopeClientException syncopeClientException = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/create",
                    userTO, UserTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            syncopeClientException =
                    e.getException(SyncopeClientExceptionType.InvalidUniques);
        }
        assertNotNull(syncopeClientException);
        assertTrue(syncopeClientException.getElements().contains("userId"));
    }

    @Test
    public void delete() {
        try {
            restTemplate.delete(BASE_URL + "user/delete/{userId}", 0);
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }

        restTemplate.delete(BASE_URL + "user/delete/{userId}", 2);
        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json",
                    UserTO.class, 2);
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public void list() {
        UserTOs users = restTemplate.getForObject(BASE_URL
                + "user/list.json", UserTOs.class);

        assertNotNull(users);
        assertEquals(4, users.getUsers().size());
    }

    @Test
    public void read() {
        UserTO userTO = restTemplate.getForObject(BASE_URL
                + "user/read/{userId}.json", UserTO.class, 1);

        assertNotNull(userTO);
        assertNotNull(userTO.getAttributes());
        assertFalse(userTO.getAttributes().isEmpty());
    }

    @Test
    public void token() {
        UserTO userTO = getSampleTO("d.e@f.com");

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        userTO = restTemplate.postForObject(BASE_URL + "user/activate",
                userTO, UserTO.class);
        assertNull(userTO.getToken());

        userTO = restTemplate.getForObject(
                BASE_URL + "user/generateToken/{userId}",
                UserTO.class, userTO.getId());
        assertNotNull(userTO.getToken());

        userTO = restTemplate.postForObject(BASE_URL + "user/verifyToken",
                userTO, UserTO.class);
        assertNull(userTO.getToken());
    }

    @Test
    public void search() {
        LeafSearchCondition usernameLeafCond1 =
                new LeafSearchCondition(LeafSearchCondition.Type.LIKE);
        usernameLeafCond1.setSchema("username");
        usernameLeafCond1.setExpression("%o%");

        LeafSearchCondition usernameLeafCond2 =
                new LeafSearchCondition(LeafSearchCondition.Type.LIKE);
        usernameLeafCond2.setSchema("username");
        usernameLeafCond2.setExpression("%i%");

        NodeSearchCondition searchCondition =
                NodeSearchCondition.getAndSearchCondition(
                NodeSearchCondition.getLeafCondition(usernameLeafCond1),
                NodeSearchCondition.getLeafCondition(usernameLeafCond2));

        assertTrue(searchCondition.checkValidity());

        UserTOs matchedUsers = restTemplate.postForObject(
                BASE_URL + "user/search",
                searchCondition, UserTOs.class);

        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.getUsers().isEmpty());
    }

    @Test
    public void update() {
        UserTO userTO = getSampleTO("g.h@t.com");

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        userTO = restTemplate.postForObject(BASE_URL + "user/activate",
                userTO, UserTO.class);

        AttributeMod attributeMod = new AttributeMod();
        attributeMod.setSchema("userId");
        attributeMod.addValueToBeRemoved("g.h@t.com");
        attributeMod.addValueToBeAdded("new@t.com");

        MembershipMod membershipMod = new MembershipMod();
        membershipMod.setRole(8L);
        membershipMod.addAttributeToBeUpdated(attributeMod);

        assertTrue(userTO.getDerivedAttributes().isEmpty());
        assertTrue(userTO.getMemberships().isEmpty());

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");
        userMod.addDerivedAttributeToBeAdded("cn");
        userMod.addAttributeToBeUpdated(attributeMod);
        userMod.addMembershipMod(membershipMod);

        userTO = restTemplate.postForObject(BASE_URL + "user/update",
                userMod, UserTO.class);

        assertEquals("newPassword", userTO.getPassword());

        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.addValue("new@t.com");

        assertTrue(userTO.getAttributes().contains(attributeTO));
        assertFalse(userTO.getDerivedAttributes().isEmpty());
        assertFalse(userTO.getMemberships().isEmpty());
    }
}
