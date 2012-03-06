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
package org.syncope.console.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.VirtualSchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.AttributableType;

/**
 * Console client for invoking rest schema services.
 */
@Component
public class SchemaRestClient extends AbstractBaseRestClient {

    /**
     * Get schemas.
     *
     * @return List of schemas.
     */
    public List<SchemaTO> getSchemas(final AttributableType type) {
        List<SchemaTO> schemas = null;

        try {
            schemas = Arrays.asList(restTemplate.getForObject(
                    baseURL + "schema/" + type.name().toLowerCase() + "/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all schemas", e);
        }

        return schemas;
    }

    /**
     * Get schema names.
     *
     * @return List of schema names.
     */
    public List<String> getSchemaNames(final AttributableType type) {
        final List<String> schemaNames = new ArrayList<String>();

        try {
            final List<SchemaTO> userSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL + "schema/" + type.name().toLowerCase() + "/list.json", SchemaTO[].class));

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
     *
     * @return List of derived schemas.
     */
    public List<DerivedSchemaTO> getDerivedSchemas(final AttributableType type) {

        List<DerivedSchemaTO> userDerivedSchemas = null;

        try {
            userDerivedSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL + "derivedSchema/" + type.name().toLowerCase() + "/list.json", DerivedSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user derived schemas", e);
        }

        return userDerivedSchemas;
    }

    /**
     * Get derived schema names.
     *
     * @return List of derived schema names.
     */
    public List<String> getDerivedSchemaNames(final AttributableType type) {

        final List<String> userDerivedSchemasNames = new ArrayList<String>();

        try {
            final List<DerivedSchemaTO> userDerivedSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL + "derivedSchema/" + type.name().toLowerCase() + "/list.json", DerivedSchemaTO[].class));

            for (DerivedSchemaTO schemaTO : userDerivedSchemas) {
                userDerivedSchemasNames.add(schemaTO.getName());
            }
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user derived schema names", e);
        }

        return userDerivedSchemasNames;
    }

    /**
     * Get derived schemas.
     *
     * @return List of derived schemas.
     */
    public List<VirtualSchemaTO> getVirtualSchemas(final AttributableType type) {

        List<VirtualSchemaTO> userVirtualSchemas = null;

        try {
            userVirtualSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL + "virtualSchema/" + type.name().toLowerCase() + "/list.json", VirtualSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user derived schemas", e);
        }

        return userVirtualSchemas;
    }

    /**
     * Get virtual schema names.
     *
     * @return List of virtual schema names.
     */
    public List<String> getVirtualSchemaNames(final AttributableType type) {
        final List<String> userVirtualSchemasNames = new ArrayList<String>();

        try {
            final List<VirtualSchemaTO> userVirtualSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL + "virtualSchema/" + type.name().toLowerCase() + "/list.json", VirtualSchemaTO[].class));

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
     *
     * @param schemaTO
     */
    public void createSchema(final AttributableType type, final SchemaTO schemaTO) {
        restTemplate.postForObject(
                baseURL + "schema/" + type.name().toLowerCase() + "/create", schemaTO, SchemaTO.class);
    }

    /**
     * Load an already existent user schema by its name.
     *
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public SchemaTO readSchema(final AttributableType type, final String name) {
        SchemaTO schema = null;

        try {
            schema = restTemplate.getForObject(
                    baseURL + "schema/" + type.name().toLowerCase() + "/read/" + name + ".json", SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a user schema", e);
        }
        return schema;
    }

    /**
     * Update an already existent user schema.
     *
     * @param schemaTO updated
     */
    public void updateSchema(final AttributableType type, SchemaTO schemaTO) {
        restTemplate.postForObject(
                baseURL + "schema/" + type.name().toLowerCase() + "/update", schemaTO, SchemaTO.class);
    }

    /**
     * Delete an already existent user schema by its name.
     *
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteSchema(final AttributableType type, String name) {
        restTemplate.delete(baseURL + "schema/" + type.name().toLowerCase() + "/delete/" + name + ".json");
    }

    /**
     * Create new derived user schema.
     *
     * @param schemaTO
     */
    public void createDerivedSchema(final AttributableType type, final DerivedSchemaTO schemaTO) {
        restTemplate.postForObject(
                baseURL + "derivedSchema/" + type.name().toLowerCase() + "/create", schemaTO, DerivedSchemaTO.class);
    }

    /**
     * Create new derived user schema.
     *
     * @param schemaTO
     */
    public void createVirtualSchema(final AttributableType type, final VirtualSchemaTO schemaTO) {
        restTemplate.postForObject(
                baseURL + "virtualSchema/" + type.name().toLowerCase() + "/create", schemaTO, VirtualSchemaTO.class);
    }

    /**
     * Load an already existent user derived schema by its name.
     *
     * @param name (e.g.:surname)
     * @return DerivedSchemaTO
     */
    public DerivedSchemaTO readDerivedSchema(final AttributableType type, final String name) {
        DerivedSchemaTO derivedSchemaTO = null;
        try {
            derivedSchemaTO = restTemplate.getForObject(
                    baseURL + "derivedSchema/" + type.name().toLowerCase() + "/read/" + name + ".json",
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a derived user schema", e);
        }
        return derivedSchemaTO;
    }

    /**
     * Update an already existent user derived schema.
     *
     * @param schemaTO updated
     */
    public void updateDerivedSchema(final AttributableType type, final DerivedSchemaTO schemaTO) {
        restTemplate.postForObject(
                baseURL + "derivedSchema/" + type.name().toLowerCase() + "/update", schemaTO, DerivedSchemaTO.class);
    }

    /**
     * Update an already existent user derived schema.
     *
     * @param schemaTO updated
     */
    public void updateVirtualSchema(final AttributableType type, final VirtualSchemaTO schemaTO) {
        restTemplate.postForObject(baseURL
                + "virtualSchema/" + type.name().toLowerCase() + "/update", schemaTO, VirtualSchemaTO.class);
    }

    /**
     * Delete an already existent user derived schema by its name.
     *
     * @param name (e.g.:surname)
     */
    public void deleteDerivedSchema(final AttributableType type, String name) {
        restTemplate.delete(baseURL + "derivedSchema/" + type.name().toLowerCase() + "/delete/" + name + ".json");
    }

    /**
     * Delete an already existent user virtual schema by its name.
     *
     * @param name (e.g.:surname)
     */
    public void deleteVirtualSchema(final AttributableType type, final String name) {
        restTemplate.delete(baseURL + "virtualSchema/" + type.name().toLowerCase() + "/delete/" + name + ".json");
    }

    /**
     * Populator for Validator Schema DropDown components.
     */
    public List<String> getAllValidatorClasses() {
        List<String> validators = null;

        try {
            validators = Arrays.asList(restTemplate.getForObject(
                    baseURL + "configuration/validators.json", String[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all validators", e);
        }
        return validators;
    }
}
