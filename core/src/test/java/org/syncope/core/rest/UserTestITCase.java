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
package org.syncope.core.rest;

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
import org.syncope.client.search.AttributeCond;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.UserTOs;
import org.syncope.client.to.WorkflowActionsTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.workflow.Constants;
import org.syncope.types.SyncopeClientExceptionType;

public class UserTestITCase extends AbstractTest {

    private UserTO getSampleTO(final String email) {
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

        AttributeTO typeTO = new AttributeTO();
        typeTO.setSchema("type");
        typeTO.addValue("a type");
        userTO.addAttribute(typeTO);

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
    public final void createWithException() {
        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.addValue("userId@nowhere.org");

        UserTO newUserTO = new UserTO();
        newUserTO.addAttribute(attributeTO);

        restTemplate.postForObject(BASE_URL + "user/create",
                newUserTO, UserTO.class);
    }

    @Test
    public final void create() {
        UserTO userTO = getSampleTO("a.b@c.com");

        // add a membership
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.addMembership(membershipTO);

        // add an attribute with no values: must be ignored
        AttributeTO nullValueAttributeTO = new AttributeTO();
        nullValueAttributeTO.setSchema("subscriptionDate");
        nullValueAttributeTO.setValues(null);
        membershipTO.addAttribute(nullValueAttributeTO);

        // add an attribute with a non-existing schema: must be ignored
        AttributeTO attrWithInvalidSchemaTO = new AttributeTO();
        attrWithInvalidSchemaTO.setSchema("invalid schema");
        attrWithInvalidSchemaTO.addValue("a value");
        userTO.addAttribute(attrWithInvalidSchemaTO);

        // add an attribute with null value: must be ignored
        nullValueAttributeTO = new AttributeTO();
        nullValueAttributeTO.setSchema("activationDate");
        //nullValueAttributeTO.setValues(null);
        nullValueAttributeTO.addValue(null);
        userTO.addAttribute(nullValueAttributeTO);

        // 1. create user
        UserTO newUserTO = restTemplate.postForObject(
                BASE_URL + "user/create?syncRoles=8",
                userTO, UserTO.class);

        assertNotNull(newUserTO);
        assertFalse(newUserTO.getAttributes().contains(
                attrWithInvalidSchemaTO));

        WorkflowActionsTO workflowActions = restTemplate.getForObject(
                BASE_URL + "user/actions/{userId}", WorkflowActionsTO.class,
                newUserTO.getId());
        assertTrue(workflowActions.getActions().equals(
                Collections.singleton(Constants.ACTION_ACTIVATE)));

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
    public final void createWithRequiredValueMissing() {
        UserTO userTO = getSampleTO("a.b@c.it");

        AttributeTO type = null;
        for (AttributeTO attr : userTO.getAttributes()) {
            if ("type".equals(attr.getSchema())) {
                type = attr;
            }
        }
        assertNotNull(type);

        userTO.removeAttribute(type);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.addMembership(membershipTO);

        SyncopeClientCompositeErrorException ex = null;
        try {
            // 1. create user without type (mandatory by UserSchema)
            restTemplate.postForObject(
                    BASE_URL + "user/create?syncRoles=8",
                    userTO, UserTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            ex = e;
        }
        assertNotNull(ex);
        assertNotNull(ex.getException(
                SyncopeClientExceptionType.RequiredValuesMissing));

        AttributeTO fType = new AttributeTO();
        fType.setSchema("type");
        fType.addValue("F");
        userTO.addAttribute(fType);

        ex = null;
        try {
            // 2. create user without firstname (mandatory when type == 'F')
            restTemplate.postForObject(
                    BASE_URL + "user/create?syncRoles=8",
                    userTO, UserTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            ex = e;
        }
        assertNotNull(ex);
        assertNotNull(ex.getException(
                SyncopeClientExceptionType.RequiredValuesMissing));
    }

    @Test
    public final void delete() {
        try {
            restTemplate.delete(BASE_URL + "user/delete/{userId}", 0);
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }

        UserTO userTO = getSampleTO("qqgf.z@nn.com");

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        userTO = restTemplate.postForObject(BASE_URL + "user/activate",
                userTO, UserTO.class);

        restTemplate.delete(BASE_URL + "user/delete/{userId}", userTO.getId());
        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json",
                    UserTO.class, userTO.getId());
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public final void list() {
        UserTOs users = restTemplate.getForObject(
                BASE_URL + "user/list.json", UserTOs.class);

        assertNotNull(users);
        assertEquals(4, users.getUsers().size());
    }

    @Test
    public final void read() {
        UserTO userTO = restTemplate.getForObject(
                BASE_URL + "user/read/{userId}.json", UserTO.class, 1);

        assertNotNull(userTO);
        assertNotNull(userTO.getAttributes());
        assertFalse(userTO.getAttributes().isEmpty());
    }

    @Test
    public final void token() {
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
    public final void search() {
        AttributeCond usernameLeafCond1 =
                new AttributeCond(AttributeCond.Type.LIKE);
        usernameLeafCond1.setSchema("username");
        usernameLeafCond1.setExpression("%o%");

        AttributeCond usernameLeafCond2 =
                new AttributeCond(AttributeCond.Type.LIKE);
        usernameLeafCond2.setSchema("username");
        usernameLeafCond2.setExpression("%i%");

        NodeCond searchCondition = NodeCond.getAndCond(
                NodeCond.getLeafCond(usernameLeafCond1),
                NodeCond.getLeafCond(usernameLeafCond2));

        assertTrue(searchCondition.checkValidity());

        UserTOs matchedUsers = restTemplate.postForObject(
                BASE_URL + "user/search",
                searchCondition, UserTOs.class);

        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.getUsers().isEmpty());
    }

    @Test
    public final void update() {
        UserTO userTO = getSampleTO("g.h@t.com");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        AttributeTO membershipAttr = new AttributeTO();
        membershipAttr.setSchema("subscriptionDate");
        membershipAttr.addValue("2009-08-18T16:33:12.203+0200");
        membershipTO.addAttribute(membershipAttr);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        userTO = restTemplate.postForObject(BASE_URL + "user/activate",
                userTO, UserTO.class);

        assertTrue(userTO.getDerivedAttributes().isEmpty());
        assertEquals(1, userTO.getMemberships().size());

        AttributeMod attributeMod = new AttributeMod();
        attributeMod.setSchema("subscriptionDate");
        attributeMod.addValueToBeAdded("2010-08-18T16:33:12.203+0200");

        MembershipMod membershipMod = new MembershipMod();
        membershipMod.setRole(8L);
        membershipMod.addAttributeToBeUpdated(attributeMod);

        attributeMod = new AttributeMod();
        attributeMod.setSchema("userId");
        attributeMod.addValueToBeAdded("t.w@spre.net");

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");
        userMod.addAttributeToBeRemoved("userId");
        userMod.addAttributeToBeUpdated(attributeMod);
        userMod.addDerivedAttributeToBeAdded("cn");
        userMod.addMembershipToBeAdded(membershipMod);
        userMod.addMembershipToBeRemoved(
                userTO.getMemberships().iterator().next().getId());

        userTO = restTemplate.postForObject(BASE_URL + "user/update",
                userMod, UserTO.class);

        assertEquals("newPassword", userTO.getPassword());
        assertEquals(1, userTO.getMemberships().size());
        assertEquals(1, userTO.getMemberships().iterator().next().
                getAttributes().size());
        assertEquals(1, userTO.getDerivedAttributes().size());
        boolean attributeFound = false;
        for (AttributeTO attributeTO : userTO.getAttributes()) {
            if ("userId".equals(attributeTO.getSchema())) {
                attributeFound = true;

                assertEquals(Collections.singleton("t.w@spre.net"),
                        attributeTO.getValues());
            }
        }
        assertTrue(attributeFound);
    }
}
