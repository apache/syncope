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

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.to.SchemaMappingTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.dao.SchemaDAO;

public class SchemaMappingTestITCase extends AbstractTestITCase {

    @Autowired
    SchemaDAO schemaDAO;

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public void createWithException() {
        final String resourceName = "ws-target-resource-1";

        SchemaMappingTOs mappings = new SchemaMappingTOs();

        SchemaMappingTO mapping = new SchemaMappingTO();

        mappings.addMapping(mapping);

        restTemplate.postForObject(
                BASE_URL + "mapping/create/{resourceName}.json",
                mappings,
                SchemaMappingTOs.class,
                resourceName);
    }

    @Test
    public void create() {
        final String resourceName = "ws-target-resource-1";

        SchemaMappingTOs mappings = new SchemaMappingTOs();

        SchemaMappingTO mapping = null;

        for (int i = 0; i < 3; i++) {
            mapping = new SchemaMappingTO();
            mapping.setField("test" + i);
            mapping.setUserSchema("username");
            mapping.setRoleSchema("icon");
            mappings.addMapping(mapping);
        }

        SchemaMappingTOs actuals =
                (SchemaMappingTOs) restTemplate.postForObject(
                BASE_URL + "mapping/create/{resourceName}.json",
                mappings, SchemaMappingTOs.class, resourceName);

        assertNotNull(actuals);

        assertTrue(actuals.getMappings().size() == 3);

        // check the non existence

        actuals = restTemplate.getForObject(
                BASE_URL + "mapping/getResourceMapping/{resourceName}.json",
                SchemaMappingTOs.class,
                resourceName);

        assertNotNull(actuals);

        assertTrue(actuals.getMappings().size() == 3);
    }

    @Test
    public void deleteWithException() {
        try {

            restTemplate.delete(
                    BASE_URL + "mapping/delete/{resourceName}.json",
                    "notfoundresourcename");

        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public void delete() {
        final String resourceName = "ws-target-resource-2";

        restTemplate.delete(
                BASE_URL + "mapping/delete/{resourceName}.json",
                resourceName);

        SchemaMappingTOs actuals = restTemplate.getForObject(
                BASE_URL + "mapping/getResourceMapping/{resourceName}.json",
                SchemaMappingTOs.class,
                resourceName);

        assertNotNull(actuals);

        assertTrue(actuals.getMappings().isEmpty());
    }

    @Test
    public void getRoleResourcesMapping(){
        final Long roleId = 3L;

        SchemaMappingTOs actuals =
                restTemplate.getForObject(
                BASE_URL + "mapping/getRoleResourcesMapping/{roleId}.json",
                SchemaMappingTOs.class,
                roleId);

        assertNotNull(actuals);

        assertFalse(actuals.getMappings().isEmpty());
    }
}
