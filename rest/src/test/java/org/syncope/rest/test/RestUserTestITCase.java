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
package org.syncope.rest.test;

import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.syncope.rest.to.AttributeTO;
import org.syncope.rest.to.SearchParameters;
import org.syncope.rest.to.UserTO;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class RestUserTestITCase {

    private static final Logger log = LoggerFactory.getLogger(RestUserTestITCase.class);
    private static final String BASE_URL = "http://localhost:8080/syncope-rest/user/";
    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void create() {
        AttributeTO attribute = new AttributeTO();
        attribute.setName("attr1");
        attribute.setValues(Collections.singleton("value1"));

        UserTO newUserTO = new UserTO();
        newUserTO.setId(0L);
        newUserTO.setAttributes(Collections.singleton(attribute));

        UserTO userTO = restTemplate.postForObject(BASE_URL + "create",
                newUserTO, UserTO.class);

        assertEquals(newUserTO, userTO);
    }

    @Test
    public void delete() {
        try {
            restTemplate.delete(BASE_URL + "delete/{userId}", "0");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public void read() {
        UserTO userTO = restTemplate.getForObject(BASE_URL + "read/{userId}.json",
                UserTO.class, "0");

        assertNotNull(userTO);

        if (log.isDebugEnabled()) {
            log.debug(userTO.toString());
        }
    }

    @Test
    public void passwordReset() {
        String tokenId = restTemplate.getForObject(BASE_URL
                + "passwordReset/{userId}.json"
                + "?passwordResetFormURL={passwordResetFormURL}"
                + "&gotoURL={gotoURL}",
                String.class, "0",
                "http://www.google.it/passwordResetForm",
                "http://www.google.it/gotoURL");

        assertNotNull(tokenId);

        restTemplate.put(BASE_URL + "passwordReset/{userId}.json"
                + "?tokenId={tokenId}&newPassword={newPassword}",
                null, "0", tokenId, "newPassword");
    }

    @Test
    public void search() {
        SearchParameters searchParameters = new SearchParameters();

        List<UserTO> matchedUsers = restTemplate.postForObject(
                BASE_URL + "search",
                searchParameters, List.class);

        assertNotNull(matchedUsers);
    }

    @Test
    public void update() {
        AttributeTO attribute = new AttributeTO();
        attribute.setName("attr1");
        attribute.setValues(Collections.singleton("value1"));

        UserTO newUserTO = new UserTO();
        newUserTO.setId(0L);
        newUserTO.setAttributes(Collections.singleton(attribute));

        UserTO userTO = restTemplate.postForObject(BASE_URL + "update",
                newUserTO, UserTO.class);

        assertEquals(newUserTO, userTO);
    }
}
