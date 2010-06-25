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
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.SearchParameters;
import org.syncope.client.to.UserTO;

public class UserTestITCase extends AbstractTestITCase {

    @Test
    public void create() {
        UserTO newUserTO = new UserTO();
        newUserTO.setAttributes(Collections.singletonMap("attr1",
                Collections.singleton("value1")));

        UserTO userTO = restTemplate.postForObject(BASE_URL + "user/create",
                newUserTO, UserTO.class);

        assertEquals(newUserTO, userTO);
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
        Set<UserTO> users = restTemplate.getForObject(BASE_URL
                + "user/list.json", Set.class);

        assertNotNull(users);
        assertEquals(4, users.size());
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

        List<UserTO> matchedUsers = restTemplate.postForObject(
                BASE_URL + "user/search",
                searchParameters, List.class);

        assertNotNull(matchedUsers);
    }

    @Test
    public void update() {
        UserTO newUserTO = new UserTO();
        newUserTO.setAttributes(Collections.singletonMap("attr1",
                Collections.singleton("value1")));

        UserTO userTO = restTemplate.postForObject(BASE_URL + "user/update",
                newUserTO, UserTO.class);

        assertEquals(newUserTO, userTO);
    }
}
