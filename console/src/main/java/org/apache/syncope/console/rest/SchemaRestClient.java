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
package org.apache.syncope.console.rest;

import java.util.ArrayList;
import java.util.List;

import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.services.SchemaService.SchemaType;
import org.apache.syncope.common.to.DerivedSchemaTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.ValidatorTO;
import org.apache.syncope.common.to.VirtualSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking rest schema services.
 */
@Component
public class SchemaRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2479730152700312373L;

    /**
     * Get schemas.
     *
     * @return List of schemas.
     */
    public List<SchemaTO> getSchemas(final AttributableType type) {
        List<SchemaTO> schemas = null;

        try {
            schemas = getService(SchemaService.class).list(type, SchemaType.NORMAL);
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
            final List<SchemaTO> schemas = getService(SchemaService.class).list(type, SchemaType.NORMAL);
            for (SchemaTO schemaTO : schemas) {
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
            userDerivedSchemas = getService(SchemaService.class).list(type, SchemaType.DERIVED);
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
            final List<DerivedSchemaTO> userDerivedSchemas = getService(SchemaService.class).list(type,
                    SchemaType.DERIVED);

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
            userVirtualSchemas = getService(SchemaService.class).list(type, SchemaType.VIRTUAL);
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
            final List<VirtualSchemaTO> userVirtualSchemas = getService(SchemaService.class).list(type,
                    SchemaType.VIRTUAL);
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
        getService(SchemaService.class).create(type, SchemaType.NORMAL, schemaTO);
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
            schema = getService(SchemaService.class).read(type, SchemaType.NORMAL, name);
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
    public void updateSchema(final AttributableType type, final SchemaTO schemaTO) {
        getService(SchemaService.class).update(type, SchemaType.NORMAL, schemaTO.getName(), schemaTO);
    }

    /**
     * Delete an already existent user schema by its name.
     *
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public SchemaTO deleteSchema(final AttributableType type, final String name) {
        SchemaTO response = getService(SchemaService.class).read(type, SchemaType.NORMAL, name);
        getService(SchemaService.class).delete(type, SchemaType.NORMAL, name);
        return response;
    }

    /**
     * Create new derived user schema.
     *
     * @param schemaTO
     */
    public void createDerivedSchema(final AttributableType type, final DerivedSchemaTO schemaTO) {
        getService(SchemaService.class).create(type, SchemaType.DERIVED, schemaTO);
    }

    /**
     * Create new derived user schema.
     *
     * @param schemaTO
     */
    public void createVirtualSchema(final AttributableType type, final VirtualSchemaTO schemaTO) {
        getService(SchemaService.class).create(type, SchemaType.VIRTUAL, schemaTO);
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
            derivedSchemaTO = getService(SchemaService.class).read(type, SchemaType.DERIVED, name);
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
        getService(SchemaService.class).update(type, SchemaType.DERIVED, schemaTO.getName(), schemaTO);
    }

    /**
     * Update an already existent user derived schema.
     *
     * @param schemaTO updated
     */
    public void updateVirtualSchema(final AttributableType type, final VirtualSchemaTO schemaTO) {
        getService(SchemaService.class).update(type, SchemaType.VIRTUAL, schemaTO.getName(), schemaTO);
    }

    /**
     * Delete an already existent user derived schema by its name.
     *
     * @param name (e.g.:surname)
     */
    public DerivedSchemaTO deleteDerivedSchema(final AttributableType type, final String name) {
        DerivedSchemaTO schemaTO = getService(SchemaService.class).read(type, SchemaType.DERIVED, name);
        getService(SchemaService.class).delete(type, SchemaType.DERIVED, name);
        return schemaTO;
    }

    /**
     * Delete an already existent user virtual schema by its name.
     *
     * @param name (e.g.:surname)
     */
    public VirtualSchemaTO deleteVirtualSchema(final AttributableType type, final String name) {
        VirtualSchemaTO schemaTO = getService(SchemaService.class).read(type, SchemaType.VIRTUAL, name);
        getService(SchemaService.class).delete(type, SchemaType.VIRTUAL, name);
        return schemaTO;
    }

    /**
     * Populator for Validator Schema DropDown components.
     */
    public List<String> getAllValidatorClasses() {
        List<String> response = null;

        try {
            List<ValidatorTO> validators = new ArrayList<ValidatorTO>(getService(ConfigurationService.class)
                    .getValidators());
            response = CollectionWrapper.unwrapValidator(validators);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all validators", e);
        }
        return response;
    }
}
