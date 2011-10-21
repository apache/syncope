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

import org.syncope.types.EntityViolationType;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.syncope.types.SyncopeClientExceptionType;
import org.syncope.client.validation.SyncopeClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.SchemaType;
import static org.junit.Assert.*;

public class SchemaTestITCase extends AbstractTest {

    @Test
    public void create() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("testAttribute");
        schemaTO.setMandatoryCondition("false");
        schemaTO.setType(SchemaType.String);

        SchemaTO newSchemaTO = restTemplate.postForObject(BASE_URL
                + "schema/user/create", schemaTO, SchemaTO.class);
        assertEquals(schemaTO, newSchemaTO);

        newSchemaTO = restTemplate.postForObject(BASE_URL
                + "schema/membership/create", schemaTO, SchemaTO.class);
        assertEquals(schemaTO, newSchemaTO);
    }

    @Test
    public final void createWithNotPermittedName() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("failedLogins");
        schemaTO.setType(SchemaType.String);

        SyncopeClientException syncopeClientException = null;

        try {
            restTemplate.postForObject(BASE_URL
                    + "schema/user/create", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            syncopeClientException = scce.getException(
                    SyncopeClientExceptionType.InvalidUSchema);

            assertNotNull(syncopeClientException.getElements());
            assertEquals(1, syncopeClientException.getElements().size());
            assertTrue(syncopeClientException.getElements().iterator().next().
                    contains(
                    EntityViolationType.InvalidUSchema.toString()));
        }

        assertNotNull(syncopeClientException);
    }

    @Test
    public final void createREnumWithoutEnumeration() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("enumcheck");
        schemaTO.setType(SchemaType.Enum);

        SyncopeClientException syncopeClientException = null;

        try {
            restTemplate.postForObject(BASE_URL
                    + "schema/role/create", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            syncopeClientException = scce.getException(
                    SyncopeClientExceptionType.InvalidRSchema);

            assertNotNull(syncopeClientException.getElements());
            assertEquals(1, syncopeClientException.getElements().size());
            assertTrue(syncopeClientException.getElements().iterator().next().
                    contains(
                    EntityViolationType.InvalidSchemaTypeSpecification.toString()));
        }

        assertNotNull(syncopeClientException);
    }

    @Test
    public final void createUEnumWithoutEnumeration() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("enumcheck");
        schemaTO.setType(SchemaType.Enum);

        SyncopeClientException syncopeClientException = null;

        try {
            restTemplate.postForObject(BASE_URL
                    + "schema/user/create", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            syncopeClientException = scce.getException(
                    SyncopeClientExceptionType.InvalidUSchema);

            assertNotNull(syncopeClientException.getElements());
            assertEquals(1, syncopeClientException.getElements().size());
            assertTrue(syncopeClientException.getElements().iterator().next().
                    contains(
                    EntityViolationType.InvalidSchemaTypeSpecification.toString()));
        }

        assertNotNull(syncopeClientException);
    }

    @Test
    public void delete() {
        restTemplate.delete(BASE_URL + "schema/user/delete/firstname.json");
        SchemaTO firstname = null;
        try {
            firstname = restTemplate.getForObject(BASE_URL
                    + "schema/user/read/firstname.json", SchemaTO.class);
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
        assertNull(firstname);
    }

    @Test
    public void list() {
        List<SchemaTO> userSchemas = Arrays.asList(
                restTemplate.getForObject(BASE_URL
                + "schema/user/list.json", SchemaTO[].class));
        assertFalse(userSchemas.isEmpty());
        for (SchemaTO schemaTO : userSchemas) {
            assertNotNull(schemaTO);
        }

        List<SchemaTO> roleSchemas = Arrays.asList(
                restTemplate.getForObject(BASE_URL
                + "schema/role/list.json", SchemaTO[].class));
        assertFalse(roleSchemas.isEmpty());
        for (SchemaTO schemaTO : roleSchemas) {
            assertNotNull(schemaTO);
        }

        List<SchemaTO> membershipSchemas = Arrays.asList(
                restTemplate.getForObject(BASE_URL
                + "schema/membership/list.json", SchemaTO[].class));
        assertFalse(membershipSchemas.isEmpty());
        for (SchemaTO schemaTO : membershipSchemas) {
            assertNotNull(schemaTO);
        }
    }

    @Test
    public void update() {
        SchemaTO schemaTO = restTemplate.getForObject(BASE_URL
                + "schema/role/read/icon.json", SchemaTO.class);
        assertNotNull(schemaTO);

        SchemaTO updatedTO = restTemplate.postForObject(BASE_URL
                + "schema/role/update", schemaTO, SchemaTO.class);
        assertEquals(schemaTO, updatedTO);

        updatedTO.setType(SchemaType.Date);
        SyncopeClientException syncopeClientException = null;

        try {
            restTemplate.postForObject(BASE_URL
                    + "schema/role/update", updatedTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            syncopeClientException = scce.getException(
                    SyncopeClientExceptionType.InvalidRSchema);
        }

        assertNotNull(syncopeClientException);
    }
}
