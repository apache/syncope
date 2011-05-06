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

import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.SyncopeClientExceptionType;
import java.util.Arrays;
import java.util.List;
import org.syncope.client.to.VirtualSchemaTO;
import org.junit.Test;
import static org.junit.Assert.*;

public class VirtualSchemaTestITCase extends AbstractTest {

    @Test
    public void list() {
        List<VirtualSchemaTO> VirtualSchemas = Arrays.asList(
                restTemplate.getForObject(BASE_URL
                + "virtualSchema/user/list.json", VirtualSchemaTO[].class));
        assertFalse(VirtualSchemas.isEmpty());
        for (VirtualSchemaTO VirtualSchemaTO : VirtualSchemas) {
            assertNotNull(VirtualSchemaTO);
        }
    }

    @Test
    public void read() {
        VirtualSchemaTO VirtualSchemaTO = restTemplate.getForObject(BASE_URL
                + "virtualSchema/membership/read/mvirtualdata.json",
                VirtualSchemaTO.class);
        assertNotNull(VirtualSchemaTO);
    }

    @Test
    public void create() {
        VirtualSchemaTO schema = new VirtualSchemaTO();
        schema.setName("virtual");

        VirtualSchemaTO actual = restTemplate.postForObject(BASE_URL
                + "virtualSchema/user/create.json",
                schema,
                VirtualSchemaTO.class);
        assertNotNull(actual);

        actual = restTemplate.getForObject(BASE_URL
                + "virtualSchema/user/read/" + actual.getName() + ".json",
                VirtualSchemaTO.class);
        assertNotNull(actual);
    }

    @Test
    public void delete() {
        VirtualSchemaTO schema = restTemplate.getForObject(BASE_URL
                + "virtualSchema/role/read/rvirtualdata.json",
                VirtualSchemaTO.class);
        assertNotNull(schema);

        restTemplate.delete(
                BASE_URL + "virtualSchema/role/delete/{schema}",
                schema.getName());

        Throwable t = null;
        try {
            schema = restTemplate.getForObject(BASE_URL
                    + "virtualSchema/role/read/rvirtualdata.json",
                    VirtualSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
            assertNotNull(e.getException(SyncopeClientExceptionType.NotFound));
        }
        assertNotNull(t);
    }
}
