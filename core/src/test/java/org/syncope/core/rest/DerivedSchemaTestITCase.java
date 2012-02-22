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
package org.syncope.core.rest;

import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import java.util.Arrays;
import java.util.List;
import org.syncope.client.to.DerivedSchemaTO;
import org.junit.Test;
import org.syncope.types.SyncopeClientExceptionType;
import static org.junit.Assert.*;

public class DerivedSchemaTestITCase extends AbstractTest {

    @Test
    public void list() {
        List<DerivedSchemaTO> derivedSchemas = Arrays.asList(
                restTemplate.getForObject(BASE_URL
                + "derivedSchema/user/list.json", DerivedSchemaTO[].class));
        assertFalse(derivedSchemas.isEmpty());
        for (DerivedSchemaTO derivedSchemaTO : derivedSchemas) {
            assertNotNull(derivedSchemaTO);
        }
    }

    @Test
    public void read() {
        DerivedSchemaTO derivedSchemaTO = restTemplate.getForObject(BASE_URL
                + "derivedSchema/user/read/cn.json", DerivedSchemaTO.class);
        assertNotNull(derivedSchemaTO);
    }

    @Test
    public void create() {
        DerivedSchemaTO schema = new DerivedSchemaTO();
        schema.setName("derived");
        schema.setExpression("derived_sx + '_' + derived_dx");

        DerivedSchemaTO actual = restTemplate.postForObject(BASE_URL
                + "derivedSchema/user/create.json",
                schema,
                DerivedSchemaTO.class);
        assertNotNull(actual);

        actual = restTemplate.getForObject(BASE_URL
                + "derivedSchema/user/read/" + actual.getName() + ".json",
                DerivedSchemaTO.class);
        assertNotNull(actual);
        assertEquals(actual.getExpression(), "derived_sx + '_' + derived_dx");
    }

    @Test
    public void delete() {
        DerivedSchemaTO schema = restTemplate.getForObject(BASE_URL
                + "derivedSchema/role/read/rderiveddata.json",
                DerivedSchemaTO.class);
        assertNotNull(schema);

        restTemplate.delete(
                BASE_URL + "derivedSchema/role/delete/{schema}",
                schema.getName());

        Throwable t = null;
        try {
            restTemplate.getForObject(BASE_URL
                    + "derivedSchema/role/read/rderiveddata.json",
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
            assertNotNull(e.getException(SyncopeClientExceptionType.NotFound));
        }
        assertNotNull(t);
    }

    @Test
    public void update() {
        DerivedSchemaTO schema = restTemplate.getForObject(BASE_URL
                + "derivedSchema/membership/read/mderiveddata.json",
                DerivedSchemaTO.class);
        assertNotNull(schema);
        assertEquals("mderived_sx + '-' + mderived_dx", schema.getExpression());

        schema.setExpression("mderived_sx + '.' + mderived_dx");

        schema = restTemplate.postForObject(BASE_URL
                + "derivedSchema/membership/update.json",
                schema,
                DerivedSchemaTO.class);
        assertNotNull(schema);

        schema = restTemplate.getForObject(BASE_URL
                + "derivedSchema/membership/read/mderiveddata.json",
                DerivedSchemaTO.class);
        assertNotNull(schema);
        assertEquals("mderived_sx + '.' + mderived_dx", schema.getExpression());
    }
}
