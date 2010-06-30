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

import org.springframework.web.client.HttpClientErrorException;
import org.syncope.client.to.DerivedSchemaTOs;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.client.to.DerivedSchemaTO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.SchemaTOs;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.types.SchemaType;
import static org.junit.Assert.*;

public class SchemaTestITCase extends AbstractTestITCase {

    @Autowired
    SchemaDAO schemaDAO;
    @Autowired
    DerivedSchemaDAO derivedSchemaDAO;

    @Test
    public void create() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("testAttribute");
        schemaTO.setMandatory(true);
        schemaTO.setType(SchemaType.String);

        SchemaTO newSchemaTO = restTemplate.postForObject(BASE_URL
                + "schema/user/create", schemaTO, SchemaTO.class);
        assertEquals(schemaTO, newSchemaTO);
    }

    @Test
    public void delete() {
        restTemplate.delete(BASE_URL + "schema/user/delete/firstname.json");
        SchemaTO username = null;
        try {
            username = restTemplate.getForObject(BASE_URL
                    + "schema/user/read/firstname.json", SchemaTO.class);
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
        assertNull(username);
    }

    @Test
    public void list() {
        SchemaTOs userSchemas =
                restTemplate.getForObject(BASE_URL
                + "schema/user/list.json", SchemaTOs.class);
        assertFalse(userSchemas.getSchemas().isEmpty());

        SchemaTOs roleSchemas = restTemplate.getForObject(BASE_URL
                + "schema/role/list.json", SchemaTOs.class);
        assertFalse(roleSchemas.getSchemas().isEmpty());
    }

    @Test
    public void derivedList() {
        DerivedSchemaTOs derivedSchemas =
                restTemplate.getForObject(BASE_URL
                + "derivedSchema/user/list.json", DerivedSchemaTOs.class);
        assertFalse(derivedSchemas.getDerivedSchemas().isEmpty());
    }

    @Test
    public void derivedRead() {
        DerivedSchemaTO derivedSchemaTO = restTemplate.getForObject(BASE_URL
                + "derivedSchema/user/read/cn.json", DerivedSchemaTO.class);
        assertNotNull(derivedSchemaTO);
    }
}
