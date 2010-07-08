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

import static org.junit.Assert.*;

import java.util.Collections;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.SearchParameters;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.UserTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.types.SyncopeClientExceptionType;

public class UserTestITCase extends AbstractTestITCase {

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public void createWithException() {
        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.setValues(Collections.singleton("userId@nowhere.org"));

        UserTO newUserTO = new UserTO();
        newUserTO.addAttribute(attributeTO);

        restTemplate.postForObject(BASE_URL + "user/create",
                newUserTO, UserTO.class);
    }

    @Test
    public void create() {
        UserTO userTO = new UserTO();

        AttributeTO usernameTO = new AttributeTO();
        usernameTO.setSchema("username");
        usernameTO.addValue("fchicchiricco");
        userTO.addAttribute(usernameTO);

        AttributeTO surnameTO = new AttributeTO();
        surnameTO.setSchema("surname");
        surnameTO.addValue("Chicchiriccò");
        userTO.addAttribute(surnameTO);

        AttributeTO userIdTO = new AttributeTO();
        userIdTO.setSchema("userId");
        userIdTO.addValue("chicchiricco@gmail.com");
        userTO.addAttribute(userIdTO);

        AttributeTO emailTO = new AttributeTO();
        emailTO.setSchema("email");
        emailTO.addValue("chicchiricco@gmail.com");
        emailTO.addValue("syncope@googlecode.com");
        userTO.addAttribute(emailTO);

        AttributeTO loginDateTO = new AttributeTO();
        loginDateTO.setSchema("loginDate");
        loginDateTO.addValue("2010-06-30");
        loginDateTO.addValue("2010-07-01");
        userTO.addAttribute(loginDateTO);

        // 1. create user
        UserTO newUserTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        userTO.setId(newUserTO.getId());
        assertEquals(userTO, userTO);

        // 2. activate user
        restTemplate.postForObject(BASE_URL + "user/activate/" + userTO.getId(),
                Collections.singletonMap("token", userTO.getToken()),
                UserTO.class);

        // 3. try (and fail) to create another user with the same surname (unique)
        userTO = new UserTO();

        usernameTO = new AttributeTO();
        usernameTO.setSchema("username");
        usernameTO.addValue("fchicchiricco");
        userTO.addAttribute(usernameTO);

        surnameTO = new AttributeTO();
        surnameTO.setSchema("surname");
        surnameTO.addValue("Martelli");
        userTO.addAttribute(surnameTO);

        userIdTO = new AttributeTO();
        userIdTO.setSchema("userId");
        userIdTO.addValue("syncope@googlecode.com");
        userTO.addAttribute(userIdTO);

        emailTO = new AttributeTO();
        emailTO.setSchema("email");
        emailTO.addValue("syncope@googlecode.com");
        userTO.addAttribute(emailTO);

        loginDateTO = new AttributeTO();
        loginDateTO.setSchema("loginDate");
        loginDateTO.addValue("2010-07-01");
        userTO.addAttribute(loginDateTO);

        SyncopeClientException syncopeClientException = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/create",
                    userTO, UserTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            syncopeClientException =
                    e.getException(SyncopeClientExceptionType.InvalidUniques);
        }
        assertNotNull(syncopeClientException);
        assertTrue(syncopeClientException.getElements().contains("username"));
        assertTrue(syncopeClientException.getElements().contains("email"));
    }

    @Test
    public void delete() {
        try {
            restTemplate.delete(BASE_URL + "user/delete/{userId}", "0");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }

        restTemplate.delete(BASE_URL + "user/delete/{userId}", "2");
        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json",
                    UserTO.class, "2");
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
                + "user/read/{userId}.json", UserTO.class, "1");

        assertNotNull(userTO);
        assertNotNull(userTO.getAttributes());
        assertFalse(userTO.getAttributes().isEmpty());
    }

    @Test
    public void passwordReset() {
        String tokenId = restTemplate.getForObject(BASE_URL + "user/"
                + "passwordReset/{userId}.json"
                + "?passwordResetFormURL={passwordResetFormURL}"
                + "&gotoURL={gotoURL}",
                String.class, "0",
                "http://www.google.it/passwordResetForm",
                "http://www.google.it/gotoURL");

        assertNotNull(tokenId);

        restTemplate.put(BASE_URL + "user/passwordReset/{userId}.json"
                + "?tokenId={tokenId}&newPassword={newPassword}",
                null, "0", tokenId, "newPassword");
    }

    @Test
    public void search() {
        SearchParameters searchParameters = new SearchParameters();

        UserTOs matchedUsers = restTemplate.postForObject(
                BASE_URL + "user/search",
                searchParameters, UserTOs.class);

        assertNotNull(matchedUsers);
        assertTrue(matchedUsers.getUsers().isEmpty());
    }

    @Test
    public void update() {
        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("attr1");
        attributeTO.setValues(Collections.singleton("value1"));

        UserTO newUserTO = new UserTO();
        newUserTO.addAttribute(attributeTO);

        UserTO userTO = restTemplate.postForObject(BASE_URL + "user/update",
                newUserTO, UserTO.class);

        assertEquals(newUserTO, userTO);
    }
}
