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
import java.util.Collections;
import java.util.List;

import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.to.AbstractSchemaTO;
import org.apache.syncope.common.to.DerSchemaTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.ValidatorTO;
import org.apache.syncope.common.to.VirSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking rest schema services.
 */
@Component
public class SchemaRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2479730152700312373L;

    public List<? extends AbstractSchemaTO> getSchemas(final AttributableType attrType, final SchemaType schemaType) {
        List<? extends AbstractSchemaTO> schemas = Collections.emptyList();

        try {
            schemas = getService(SchemaService.class).list(attrType, schemaType);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all schemas for {} and {}", attrType, schemaType, e);
        }

        return schemas;
    }

    /**
     * Get schemas.
     *
     * @return List of schemas.
     */
    public List<SchemaTO> getSchemas(final AttributableType type) {
        List<SchemaTO> schemas = null;

        try {
            schemas = getService(SchemaService.class).list(type, SchemaType.NORMAL);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all schemas", e);
        }

        return schemas;
    }

    public List<String> getSchemaNames(final AttributableType attrType, final SchemaType schemaType) {
        final List<String> schemaNames = new ArrayList<String>();

        try {
            final List<? extends AbstractSchemaTO> schemas = getSchemas(attrType, schemaType);
            for (AbstractSchemaTO schemaTO : schemas) {
                schemaNames.add(schemaTO.getName());
            }
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user schema names", e);
        }

        return schemaNames;
    }

    /**
     * Get schema names.
     *
     * @return List of schema names.
     */
    public List<String> getSchemaNames(final AttributableType type) {
        final List<String> schemaNames = new ArrayList<String>();

        try {
            final List<SchemaTO> schemas = getSchemas(type);
            for (SchemaTO schemaTO : schemas) {
                schemaNames.add(schemaTO.getName());
            }
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user schema names", e);
        }

        return schemaNames;
    }

    /**
     * Get derived schemas.
     *
     * @return List of derived schemas.
     */
    @SuppressWarnings("unchecked")
    public List<DerSchemaTO> getDerSchemas(final AttributableType type) {
        List<DerSchemaTO> userDerSchemas = null;

        try {
            userDerSchemas = getService(SchemaService.class).list(type, SchemaType.DERIVED);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user derived schemas", e);
        }

        return userDerSchemas;
    }

    /**
     * Get derived schema names.
     *
     * @return List of derived schema names.
     */
    public List<String> getDerSchemaNames(final AttributableType type) {
        final List<String> userDerSchemasNames = new ArrayList<String>();

        try {
            final List<DerSchemaTO> userDerSchemas = getService(SchemaService.class).list(type, SchemaType.DERIVED);

            for (DerSchemaTO schemaTO : userDerSchemas) {
                userDerSchemasNames.add(schemaTO.getName());
            }
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user derived schema names", e);
        }

        return userDerSchemasNames;
    }

    /**
     * Get derived schemas.
     *
     * @return List of derived schemas.
     */
    @SuppressWarnings("unchecked")
    public List<VirSchemaTO> getVirSchemas(final AttributableType type) {
        List<VirSchemaTO> userVirSchemas = null;

        try {
            userVirSchemas = getService(SchemaService.class).list(type, SchemaType.VIRTUAL);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user derived schemas", e);
        }

        return userVirSchemas;
    }

    /**
     * Get virtual schema names.
     *
     * @return List of virtual schema names.
     */
    public List<String> getVirSchemaNames(final AttributableType type) {
        final List<String> userVirSchemasNames = new ArrayList<String>();

        try {
            @SuppressWarnings("unchecked")
            final List<VirSchemaTO> userVirSchemas = getService(SchemaService.class).list(type, SchemaType.VIRTUAL);
            for (VirSchemaTO schemaTO : userVirSchemas) {
                userVirSchemasNames.add(schemaTO.getName());
            }
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user derived schema names", e);
        }

        return userVirSchemasNames;
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
        } catch (SyncopeClientException e) {
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
    public void createDerSchema(final AttributableType type, final DerSchemaTO schemaTO) {
        getService(SchemaService.class).create(type, SchemaType.DERIVED, schemaTO);
    }

    /**
     * Create new derived user schema.
     *
     * @param schemaTO
     */
    public void createVirSchema(final AttributableType type, final VirSchemaTO schemaTO) {
        getService(SchemaService.class).create(type, SchemaType.VIRTUAL, schemaTO);
    }

    /**
     * Load an already existent user derived schema by its name.
     *
     * @param name (e.g.:surname)
     * @return DerSchemaTO
     */
    public DerSchemaTO readDerSchema(final AttributableType type, final String name) {
        DerSchemaTO derivedSchemaTO = null;
        try {
            derivedSchemaTO = getService(SchemaService.class).read(type, SchemaType.DERIVED, name);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a derived user schema", e);
        }
        return derivedSchemaTO;
    }

    /**
     * Update an already existent user derived schema.
     *
     * @param schemaTO updated
     */
    public void updateDerSchema(final AttributableType type, final DerSchemaTO schemaTO) {
        getService(SchemaService.class).update(type, SchemaType.DERIVED, schemaTO.getName(), schemaTO);
    }

    /**
     * Update an already existent user derived schema.
     *
     * @param schemaTO updated
     */
    public void updateVirSchema(final AttributableType type, final VirSchemaTO schemaTO) {
        getService(SchemaService.class).update(type, SchemaType.VIRTUAL, schemaTO.getName(), schemaTO);
    }

    /**
     * Delete an already existent user derived schema by its name.
     *
     * @param name (e.g.:surname)
     */
    public DerSchemaTO deleteDerSchema(final AttributableType type, final String name) {
        DerSchemaTO schemaTO = getService(SchemaService.class).read(type, SchemaType.DERIVED, name);
        getService(SchemaService.class).delete(type, SchemaType.DERIVED, name);
        return schemaTO;
    }

    /**
     * Delete an already existent user virtual schema by its name.
     *
     * @param name (e.g.:surname)
     */
    public VirSchemaTO deleteVirSchema(final AttributableType type, final String name) {
        VirSchemaTO schemaTO = getService(SchemaService.class).read(type, SchemaType.VIRTUAL, name);
        getService(SchemaService.class).delete(type, SchemaType.VIRTUAL, name);
        return schemaTO;
    }

    /**
     * Populator for Validator Schema DropDown components.
     */
    public List<String> getAllValidatorClasses() {
        List<String> response = null;

        try {
            response = CollectionWrapper.unwrap(
                    new ArrayList<ValidatorTO>(getService(ConfigurationService.class).getValidators()));
        } catch (SyncopeClientException e) {
            LOG.error("While getting all validators", e);
        }
        return response;
    }
}
