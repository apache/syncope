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

import org.apache.syncope.to.VirtualSchemaTO;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class VirtualSchemaTestITCase extends AbstractTest {

    @Override
    public void setupService() {
    }

    @Test
    public void list() {
        List<VirtualSchemaTO> VirtualSchemas = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "virtualSchema/user/list.json", VirtualSchemaTO[].class));
        assertFalse(VirtualSchemas.isEmpty());
        for (VirtualSchemaTO VirtualSchemaTO : VirtualSchemas) {
            assertNotNull(VirtualSchemaTO);
        }
    }

    @Test
    public void read() {
        VirtualSchemaTO VirtualSchemaTO = restTemplate.getForObject(BASE_URL
                + "virtualSchema/membership/read/mvirtualdata.json", VirtualSchemaTO.class);
        assertNotNull(VirtualSchemaTO);
    }

    @Test
    public void create() {
        VirtualSchemaTO schema = new VirtualSchemaTO();
        schema.setName("virtual");

        VirtualSchemaTO actual = restTemplate.postForObject(BASE_URL + "virtualSchema/user/create.json", schema,
                VirtualSchemaTO.class);
        assertNotNull(actual);

        actual = restTemplate.getForObject(BASE_URL + "virtualSchema/user/read/" + actual.getName() + ".json",
                VirtualSchemaTO.class);
        assertNotNull(actual);
    }

    @Test
    public void delete() {
        VirtualSchemaTO schema = restTemplate.getForObject(BASE_URL + "virtualSchema/role/read/rvirtualdata.json",
                VirtualSchemaTO.class);
        assertNotNull(schema);

        VirtualSchemaTO deletedSchema =
            restTemplate.getForObject(BASE_URL + "virtualSchema/role/delete/{schema}", VirtualSchemaTO.class,
                    schema.getName());
        assertNotNull(deletedSchema);

        Throwable t = null;
        try {
            schema = restTemplate.getForObject(BASE_URL + "virtualSchema/role/read/rvirtualdata.json",
                    VirtualSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
            assertNotNull(e.getException(SyncopeClientExceptionType.NotFound));
        }
        assertNotNull(t);
    }
}
