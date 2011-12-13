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
package org.syncope.console.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.VirtualSchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking rest schema services.
 */
@Component
public class SchemaRestClient extends AbstractBaseRestClient {

    /**
     * Get schemas.
     * @return List of schemas.
     */
    public List<SchemaTO> getSchemas(final String kind) {
        List<SchemaTO> userSchemas = null;

        try {
            userSchemas = Arrays.asList(
                    restTemplate.getForObject(
                    baseURL + "schema/" + kind + "/list.json",
                    SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user schemas", e);
        }

        return userSchemas;
    }

    /**
     * Get schema names.
     * @return List of schema names.
     */
    public List<String> getSchemaNames(final String kind) {
        final List<String> schemaNames = new ArrayList<String>();

        try {
            final List<SchemaTO> userSchemas = Arrays.asList(
                    restTemplate.getForObject(baseURL
                    + "schema/" + kind + "/list.json", SchemaTO[].class));

            for (SchemaTO schemaTO : userSchemas) {
                schemaNames.add(schemaTO.getName());
            }
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user schema names", e);
        }

        return schemaNames;
    }

    /**
     * Get derived schemas.
     * @return List of derived schemas.
     */
    public List<DerivedSchemaTO> getDerivedSchemas(final String kind) {

        List<DerivedSchemaTO> userDerivedSchemas = null;

        try {
            userDerivedSchemas = Arrays.asList(
                    restTemplate.getForObject(
                    baseURL + "derivedSchema/" + kind + "/list.json",
                    DerivedSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user derived schemas", e);
        }

        return userDerivedSchemas;
    }

    /**
     * Get derived schemas.
     * @return List of derived schemas.
     */
    public List<VirtualSchemaTO> getVirtualSchemas(final String kind) {

        List<VirtualSchemaTO> userVirtualSchemas = null;

        try {
            userVirtualSchemas = Arrays.asList(
                    restTemplate.getForObject(
                    baseURL + "virtualSchema/" + kind + "/list.json",
                    VirtualSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user derived schemas", e);
        }

        return userVirtualSchemas;
    }

    /**
     * Get derived schema names.
     * @return List of derived schema names.
     */
    public List<String> getDerivedSchemaNames(final String kind) {

        final List<String> userDerivedSchemasNames = new ArrayList<String>();

        try {
            final List<DerivedSchemaTO> userDerivedSchemas =
                    Arrays.asList(restTemplate.getForObject(baseURL
                    + "derivedSchema/" + kind + "/list.json",
                    DerivedSchemaTO[].class));

            for (DerivedSchemaTO schemaTO : userDerivedSchemas) {
                userDerivedSchemasNames.add(schemaTO.getName());
            }

        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user derived schema names", e);
        }

        return userDerivedSchemasNames;
    }

    /**
     * Get virtual schema names.
     * @return List of virtual schema names.
     */
    public List<String> getVirtualSchemaNames(final String kind) {

        final List<String> userVirtualSchemasNames = new ArrayList<String>();

        try {
            final List<VirtualSchemaTO> userVirtualSchemas =
                    Arrays.asList(restTemplate.getForObject(baseURL
                    + "virtualSchema/" + kind + "/list.json",
                    VirtualSchemaTO[].class));

            for (VirtualSchemaTO schemaTO : userVirtualSchemas) {
                userVirtualSchemasNames.add(schemaTO.getName());
            }

        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user derived schema names", e);
        }

        return userVirtualSchemasNames;
    }

    /**
     * Create new user schema.
     * @param schemaTO
     */
    public void createSchema(final String kind, final SchemaTO schemaTO) {
        restTemplate.postForObject(baseURL
                + "schema/" + kind + "/create", schemaTO, SchemaTO.class);
    }

    /**
     * Load an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public SchemaTO readSchema(final String kind, final String name) {
        SchemaTO schema = null;

        try {
            schema = restTemplate.getForObject(
                    baseURL
                    + "schema/" + kind + "/read/" + name + ".json",
                    SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a user schema", e);
        }
        return schema;
    }

    /**
     * Update an already existent user schema.
     * @param schemaTO updated
     */
    public void updateSchema(String kind, SchemaTO schemaTO) {
        restTemplate.postForObject(baseURL
                + "schema/" + kind + "/update", schemaTO, SchemaTO.class);
    }

    /**
     * Delete an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteSchema(String kind, String name) {
        restTemplate.delete(baseURL
                + "schema/" + kind + "/delete/" + name + ".json");
    }

    /**
     * Create new derived user schema.
     * @param schemaTO
     */
    public void createDerivedSchema(final String kind,
            final DerivedSchemaTO schemaTO) {

        restTemplate.postForObject(baseURL
                + "derivedSchema/" + kind + "/create", schemaTO,
                DerivedSchemaTO.class);
    }

    /**
     * Create new derived user schema.
     * @param schemaTO
     */
    public void createVirtualSchema(final String kind,
            final VirtualSchemaTO schemaTO) {

        restTemplate.postForObject(baseURL
                + "virtualSchema/" + kind + "/create", schemaTO,
                VirtualSchemaTO.class);
    }

    /**
     * Load an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return DerivedSchemaTO
     */
    public DerivedSchemaTO readDerivedSchema(final String kind,
            final String name) {

        DerivedSchemaTO derivedSchemaTO = null;
        try {
            derivedSchemaTO = restTemplate.getForObject(
                    baseURL
                    + "derivedSchema/" + kind + "/read/" + name + ".json",
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a derived user schema", e);
        }
        return derivedSchemaTO;
    }

    /**
     * Update an already existent user derived schema.
     * @param schemaTO updated
     */
    public void updateDerivedSchema(final String kind,
            final DerivedSchemaTO schemaTO) {

        restTemplate.postForObject(baseURL
                + "derivedSchema/" + kind + "/update", schemaTO,
                DerivedSchemaTO.class);
    }

    /**
     * Update an already existent user derived schema.
     * @param schemaTO updated
     */
    public void updateVirtualSchema(final String kind,
            final VirtualSchemaTO schemaTO) {

        restTemplate.postForObject(baseURL
                + "virtualSchema/" + kind + "/update", schemaTO,
                VirtualSchemaTO.class);
    }

    /**
     * Delete an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     */
    public void deleteDerivedSchema(String kind, String name) {
        try {
            restTemplate.delete(baseURL
                    + "derivedSchema/" + kind + "/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deleting a derived user schema", e);
        }
    }

    /**
     * Delete an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     */
    public void deleteVirtualSchema(final String kind,
            final String name) {

        restTemplate.delete(baseURL
                + "virtualSchema/" + kind + "/delete/" + name + ".json");
    }

    /**
     * Populator for Validator Schema DropDown components.
     */
    public List<String> getAllValidatorClasses() {
        List<String> validators = null;

        try {
            validators = Arrays.asList(restTemplate.getForObject(
                    baseURL + "configuration/validators.json",
                    String[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all validators", e);
        }
        return validators;
    }
}
