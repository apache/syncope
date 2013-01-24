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
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.syncope.common.to.ConfigurationTO;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@FixMethodOrder(MethodSorters.JVM)
public class ConfigurationTestITCase extends AbstractTest {

    // Enable running test more than once with parameters
    public ConfigurationTestITCase(String contentType) {
        super(contentType);
     }

    @Test
    public void create() {
        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setKey("testKey");
        configurationTO.setValue("testValue");

        Response response = configurationService.create(configurationTO);
        assertNotNull(response);
        assertEquals(org.apache.http.HttpStatus.SC_CREATED, response.getStatus());
        ConfigurationTO newConfigurationTO = getObject(response.getLocation(), ConfigurationTO.class, configurationService);
        assertEquals(configurationTO, newConfigurationTO);
    }

    @Test
    public void delete() throws UnsupportedEncodingException {

        try {
            configurationService.delete("nonExistent");
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }

        ConfigurationTO tokenLengthTO = configurationService.read("token.length");

        configurationService.delete("token.length");
        try {
            configurationService.read("token.length");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }

        Response response = configurationService.create(tokenLengthTO);
        assertEquals(org.apache.http.HttpStatus.SC_CREATED, response.getStatus());
        assertNotNull(response);
        ConfigurationTO newConfigurationTO = getObject(response.getLocation(), ConfigurationTO.class, configurationService);
        assertEquals(tokenLengthTO, newConfigurationTO);
    }

    @Test
    public void list() {
        List<ConfigurationTO> configurations = configurationService.list();
        assertNotNull(configurations);
        for (ConfigurationTO configuration : configurations) {
            assertNotNull(configuration);
        }
    }

    @Test
    public void read() {
        ConfigurationTO configurationTO = configurationService.read("token.expireTime");

        assertNotNull(configurationTO);
    }

    @Test
    public void update() {
        ConfigurationTO configurationTO = configurationService.read("token.expireTime");
        int value = Integer.parseInt(configurationTO.getValue());
        value++;
        configurationTO.setValue(value + "");

        configurationService.update(configurationTO.getKey(), configurationTO);
        ConfigurationTO newConfigurationTO = configurationService.read(configurationTO.getKey());
        assertEquals(configurationTO, newConfigurationTO);

        newConfigurationTO = configurationService.read("token.expireTime");
        assertEquals(configurationTO, newConfigurationTO);
    }
}
