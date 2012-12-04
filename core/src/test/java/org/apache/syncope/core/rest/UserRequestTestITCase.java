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
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.search.AttributeCond;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.to.ConfigurationTO;
import org.apache.syncope.to.UserRequestTO;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.validation.SyncopeClientException;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

public class UserRequestTestITCase extends AbstractTest {

    @Override
    public void setupService() {
    }

    @Test
    public void selfRead() {
        PreemptiveAuthHttpRequestFactory requestFactory = ((PreemptiveAuthHttpRequestFactory) restTemplate.
                getRequestFactory());
        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials("user1", "password"));

        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, 1);
            fail();
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }

        UserTO userTO = restTemplate.getForObject(BASE_URL + "user/request/read/self", UserTO.class);
        assertEquals("user1", userTO.getUsername());
    }

    @Test
    public void create() {
        // 1. set create request not allowed
        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setKey("createRequest.allowed");
        configurationTO.setValue("false");

        configurationTO = restTemplate.postForObject(BASE_URL + "configuration/create", configurationTO,
                ConfigurationTO.class);
        assertNotNull(configurationTO);

        UserTO userTO = AbstractUserTestITCase.getSampleTO("selfcreate@syncope.apache.org");

        // 2. get unauthorized when trying to request user create
        SyncopeClientException exception = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/request/create", userTO, UserRequestTO.class);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // 3. set create request allowed
        configurationTO.setValue("true");

        configurationTO = restTemplate.postForObject(BASE_URL + "configuration/create", configurationTO,
                ConfigurationTO.class);
        assertNotNull(configurationTO);

        // 4. as anonymous, request user create works
        UserRequestTO request = anonymousRestTemplate().postForObject(BASE_URL + "user/request/create", userTO,
                UserRequestTO.class);
        assertNotNull(request);

        // 5. switch back to admin
        super.resetRestTemplate();

        // 6. try to find user
        AttributeCond attrCond = new AttributeCond(AttributeCond.Type.EQ);
        attrCond.setSchema("userId");
        attrCond.setExpression("selfcreate@syncope.apache.org");

        final List<UserTO> matchingUsers = Arrays.asList(restTemplate.postForObject(BASE_URL + "user/search", NodeCond.
                getLeafCond(attrCond), UserTO[].class));
        assertTrue(matchingUsers.isEmpty());

        // 7. actually create user
        userTO = restTemplate.postForObject(BASE_URL + "user/create", request.getUserTO(), UserTO.class);
        assertNotNull(userTO);
    }

    @Test
    public void update() {
        // 1. create an user (as admin)
        UserTO userTO = AbstractUserTestITCase.getSampleTO("selfupdate@syncope.apache.org");
        String initialPassword = userTO.getPassword();

        userTO = restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword(initialPassword);

        // 2. try to request user update as admin: failure
        SyncopeClientException exception = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/request/update", userMod, UserRequestTO.class);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // 3. auth as user just created
        PreemptiveAuthHttpRequestFactory requestFactory = ((PreemptiveAuthHttpRequestFactory) restTemplate.
                getRequestFactory());
        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials(userTO.getUsername(), initialPassword));

        // 4. update with same password: not matching password policy
        exception = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/request/update", userMod, UserRequestTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            exception = scce.getException(SyncopeClientExceptionType.InvalidSyncopeUser);
        }
        assertNotNull(exception);

        // 5. now request user update works
        userMod.setPassword("new" + initialPassword);
        UserRequestTO request = restTemplate.postForObject(BASE_URL + "user/request/update", userMod,
                UserRequestTO.class);
        assertNotNull(request);

        // 6. switch back to admin
        super.resetRestTemplate();

        // 7. user password has not changed yet
        Boolean verify = restTemplate.getForObject(BASE_URL + "user/verifyPassword/{username}.json?password="
                + userMod.getPassword(), Boolean.class, userTO.getUsername());
        assertFalse(verify);

        // 8. actually update user
        userTO = restTemplate.postForObject(BASE_URL + "user/update", userMod, UserTO.class);
        assertNotNull(userTO);

        // 9. user password has now changed
        verify = restTemplate.getForObject(BASE_URL + "user/verifyPassword/{username}.json?password=" + userMod.getPassword(),
                Boolean.class, userTO.getUsername());
        assertTrue(verify);
    }

    @Test
    public void delete() {
        // 1. create an user (as admin)
        UserTO userTO = AbstractUserTestITCase.getSampleTO("selfdelete@syncope.apache.org");
        String initialPassword = userTO.getPassword();

        userTO = restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        // 2. try to request user delete as admin: failure
        SyncopeClientException exception = null;
        try {
            restTemplate.getForObject(BASE_URL + "user/request/delete/{userId}", UserRequestTO.class, userTO.getId());
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // 3. auth as user just created
        PreemptiveAuthHttpRequestFactory requestFactory = ((PreemptiveAuthHttpRequestFactory) restTemplate.
                getRequestFactory());
        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials(userTO.getUsername(), initialPassword));

        // 4. now request user delete works
        UserRequestTO request = restTemplate.getForObject(BASE_URL + "user/request/delete/{userId}",
                UserRequestTO.class, userTO.getId());
        assertNotNull(request);

        // 5. switch back to admin
        super.resetRestTemplate();

        // 6. user still exists
        UserTO actual = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
        assertNotNull(actual);

        // 7. actually delete user
        restTemplate.getForObject(BASE_URL + "user/delete/{userId}", UserTO.class, userTO.getId());

        // 8. user does not exist any more
        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
            fail();
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
}
