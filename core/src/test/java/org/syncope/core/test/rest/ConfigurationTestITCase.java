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

import java.io.UnsupportedEncodingException;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.ConfigurationTOs;

public class ConfigurationTestITCase extends AbstractTestITCase {

    @Test
    public void create() {
        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setConfKey("testKey");
        configurationTO.setConfValue("testValue");

        ConfigurationTO newConfigurationTO = restTemplate.postForObject(
                BASE_URL + "configuration/create",
                configurationTO, ConfigurationTO.class);
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

        ConfigurationTO tokenLengthTO = restTemplate.getForObject(
                BASE_URL + "configuration/read/{confKey}.json",
                ConfigurationTO.class,
                "token.length");

        restTemplate.delete(BASE_URL + "configuration/delete/{confKey}.json",
                "token.length");
        try {
            restTemplate.getForObject(
                    BASE_URL + "configuration/read/{confKey}.json",
                    ConfigurationTO.class,
                    "token.length");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }

        ConfigurationTO newConfigurationTO = restTemplate.postForObject(
                BASE_URL + "configuration/create",
                tokenLengthTO, ConfigurationTO.class);
        assertEquals(tokenLengthTO, newConfigurationTO);
    }

    @Test
    public void list() {
        ConfigurationTOs configurations = restTemplate.getForObject(
                BASE_URL + "configuration/list.json",
                ConfigurationTOs.class);

        assertNotNull(configurations);
    }

    @Test
    public void read() {
        ConfigurationTO configurationTO = restTemplate.getForObject(BASE_URL
                + "configuration/read/{confKey}.json",
                ConfigurationTO.class, "token.expireTime");

        assertNotNull(configurationTO);
    }

    @Test
    public void update() {
        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setConfKey("token.expireTime");
        configurationTO.setConfValue("61");

        ConfigurationTO newConfigurationTO = restTemplate.postForObject(
                BASE_URL + "configuration/update",
                configurationTO, ConfigurationTO.class);

        assertEquals(configurationTO, newConfigurationTO);
    }
}
