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

import org.junit.Test;
import org.syncope.types.SyncopeClientExceptionType;
import org.syncope.client.validation.SyncopeClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.SchemaTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.SchemaValueType;
import static org.junit.Assert.*;

public class SchemaTestITCase extends AbstractTest {

    @Test
    public void create() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("testAttribute");
        schemaTO.setMandatoryCondition("true");
        schemaTO.setType(SchemaValueType.String);

        SchemaTO newSchemaTO = restTemplate.postForObject(BASE_URL
                + "schema/user/create", schemaTO, SchemaTO.class);
        assertEquals(schemaTO, newSchemaTO);


        newSchemaTO = restTemplate.postForObject(BASE_URL
                + "schema/membership/create", schemaTO, SchemaTO.class);
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

        restTemplate.delete(BASE_URL
                + "schema/membership/delete/subscriptionDate.json");
        SchemaTO subscriptionDate = null;
        try {
            subscriptionDate = restTemplate.getForObject(BASE_URL
                    + "schema/membership/read/firstname.json", SchemaTO.class);
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
        assertNull(subscriptionDate);
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

        SchemaTOs membershipSchemas = restTemplate.getForObject(BASE_URL
                + "schema/membership/list.json", SchemaTOs.class);
        assertFalse(membershipSchemas.getSchemas().isEmpty());
    }

    @Test
    public void update() {
        SchemaTO schemaTO = restTemplate.getForObject(BASE_URL
                + "schema/role/read/icon.json", SchemaTO.class);
        assertNotNull(schemaTO);

        schemaTO.setVirtual(true);
        SchemaTO updatedTO = restTemplate.postForObject(BASE_URL
                + "schema/role/update", schemaTO, SchemaTO.class);
        assertEquals(schemaTO, updatedTO);

        updatedTO.setType(SchemaValueType.Date);
        SyncopeClientException syncopeClientException = null;
        try {
            restTemplate.postForObject(BASE_URL
                    + "schema/role/update", updatedTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            syncopeClientException = scce.getException(
                    SyncopeClientExceptionType.InvalidUpdate);
        }
        assertNotNull(syncopeClientException);
    }
}
