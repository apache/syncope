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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.KeyValueTO;

public class ConfigurationTestITCase extends AbstractTest {

    @Test
    public void create() {
        KeyValueTO configurationTO = new KeyValueTO();
        configurationTO.setKey("testKey");
        configurationTO.setValue("testValue");

        KeyValueTO newConfigurationTO = restTemplate.postForObject(
                BASE_URL + "configuration/create",
                configurationTO, KeyValueTO.class);
        assertEquals(configurationTO, newConfigurationTO);
    }

    @Test
    public void delete() throws UnsupportedEncodingException {
        try {
            restTemplate.delete(BASE_URL + "configuration/delete/{confKey}.json",
                    "nonExistent");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }

        KeyValueTO tokenLengthTO = restTemplate.getForObject(
                BASE_URL + "configuration/read/{confKey}.json",
                KeyValueTO.class,
                "token.length");

        restTemplate.delete(BASE_URL + "configuration/delete/{confKey}.json",
                "token.length");
        try {
            restTemplate.getForObject(
                    BASE_URL + "configuration/read/{confKey}.json",
                    KeyValueTO.class,
                    "token.length");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }

        KeyValueTO newConfigurationTO = restTemplate.postForObject(
                BASE_URL + "configuration/create",
                tokenLengthTO, KeyValueTO.class);
        assertEquals(tokenLengthTO, newConfigurationTO);
    }

    @Test
    public void list() {
        List<KeyValueTO> configurations = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "configuration/list.json",
                KeyValueTO[].class));
        assertNotNull(configurations);
        for (KeyValueTO configuration : configurations) {
            assertNotNull(configuration);
        }
    }

    @Test
    public void read() {
        KeyValueTO configurationTO = restTemplate.getForObject(BASE_URL
                + "configuration/read/{confKey}.json",
                KeyValueTO.class, "token.expireTime");

        assertNotNull(configurationTO);
    }

    @Test
    public void update() {
        KeyValueTO configurationTO = new KeyValueTO();
        configurationTO.setKey("token.expireTime");
        configurationTO.setValue("61");

        KeyValueTO newConfigurationTO = restTemplate.postForObject(
                BASE_URL + "configuration/update",
                configurationTO, KeyValueTO.class);

        assertEquals(configurationTO, newConfigurationTO);
    }
}
