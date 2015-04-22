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
package org.apache.syncope.client.console.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.AttrLayoutType;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking rest schema services.
 */
@Component
public class SchemaRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2479730152700312373L;

    public void filter(
            final List<? extends AbstractSchemaTO> schemaTOs, final Collection<String> allowed, final boolean exclude) {

        for (ListIterator<? extends AbstractSchemaTO> itor = schemaTOs.listIterator(); itor.hasNext();) {
            AbstractSchemaTO schema = itor.next();
            if (exclude) {
                if (!allowed.contains(schema.getKey())) {
                    itor.remove();
                }
            } else {
                if (allowed.contains(schema.getKey())) {
                    itor.remove();
                }
            }
        }
    }

    public List<? extends AbstractSchemaTO> getSchemas(final AttributableType attrType, final SchemaType schemaType) {
        List<? extends AbstractSchemaTO> schemas = Collections.emptyList();

        try {
            schemas = getService(SchemaService.class).list(attrType, schemaType);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all schemas for {} and {}", attrType, schemaType, e);
        }

        if (attrType == AttributableType.CONFIGURATION) {
            filter(schemas, AttrLayoutType.confKeys(), false);
        }

        return schemas;
    }

    public List<PlainSchemaTO> getSchemas(final AttributableType type) {
        List<PlainSchemaTO> schemas = null;

        try {
            schemas = getService(SchemaService.class).list(type, SchemaType.PLAIN);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all schemas", e);
        }

        return schemas;
    }

    public List<String> getSchemaNames(final AttributableType attrType, final SchemaType schemaType) {
        List<String> schemaNames = new ArrayList<>();

        try {
            CollectionUtils.collect(getSchemas(attrType, schemaType), new Transformer<AbstractSchemaTO, String>() {

                @Override
                public String transform(final AbstractSchemaTO schemaTO) {
                    return schemaTO.getKey();
                }
            }, schemaNames);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user schema names", e);
        }

        return schemaNames;
    }

    public List<String> getPlainSchemaNames(final AttributableType type) {
        return getSchemaNames(type, SchemaType.PLAIN);
    }

    public List<DerSchemaTO> getDerSchemas(final AttributableType type) {
        List<DerSchemaTO> userDerSchemas = null;

        try {
            userDerSchemas = getService(SchemaService.class).list(type, SchemaType.DERIVED);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user derived schemas", e);
        }

        return userDerSchemas;
    }

    public List<String> getDerSchemaNames(final AttributableType type) {
        return getSchemaNames(type, SchemaType.DERIVED);
    }

    public List<VirSchemaTO> getVirSchemas(final AttributableType type) {
        List<VirSchemaTO> userVirSchemas = null;

        try {
            userVirSchemas = getService(SchemaService.class).list(type, SchemaType.VIRTUAL);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all {} virtual schemas", type, e);
        }

        return userVirSchemas;
    }

    public List<String> getVirSchemaNames(final AttributableType type) {
        return getSchemaNames(type, SchemaType.VIRTUAL);
    }

    public void createPlainSchema(final AttributableType type, final PlainSchemaTO schemaTO) {
        getService(SchemaService.class).create(type, SchemaType.PLAIN, schemaTO);
    }

    public PlainSchemaTO readPlainSchema(final AttributableType type, final String name) {
        PlainSchemaTO schema = null;

        try {
            schema = getService(SchemaService.class).read(type, SchemaType.PLAIN, name);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a user schema", e);
        }
        return schema;
    }

    public void updatePlainSchema(final AttributableType type, final PlainSchemaTO schemaTO) {
        getService(SchemaService.class).update(type, SchemaType.PLAIN, schemaTO.getKey(), schemaTO);
    }

    public PlainSchemaTO deletePlainSchema(final AttributableType type, final String name) {
        PlainSchemaTO response = getService(SchemaService.class).read(type, SchemaType.PLAIN, name);
        getService(SchemaService.class).delete(type, SchemaType.PLAIN, name);
        return response;
    }

    public void createDerSchema(final AttributableType type, final DerSchemaTO schemaTO) {
        getService(SchemaService.class).create(type, SchemaType.DERIVED, schemaTO);
    }

    public DerSchemaTO readDerSchema(final AttributableType type, final String name) {
        DerSchemaTO derivedSchemaTO = null;
        try {
            derivedSchemaTO = getService(SchemaService.class).read(type, SchemaType.DERIVED, name);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a derived user schema", e);
        }
        return derivedSchemaTO;
    }

    public void updateVirSchema(final AttributableType type, final VirSchemaTO schemaTO) {
        getService(SchemaService.class).update(type, SchemaType.VIRTUAL, schemaTO.getKey(), schemaTO);
    }

    public DerSchemaTO deleteDerSchema(final AttributableType type, final String name) {
        DerSchemaTO schemaTO = getService(SchemaService.class).read(type, SchemaType.DERIVED, name);
        getService(SchemaService.class).delete(type, SchemaType.DERIVED, name);
        return schemaTO;
    }

    public void createVirSchema(final AttributableType type, final VirSchemaTO schemaTO) {
        getService(SchemaService.class).create(type, SchemaType.VIRTUAL, schemaTO);
    }

    public void updateDerSchema(final AttributableType type, final DerSchemaTO schemaTO) {
        getService(SchemaService.class).update(type, SchemaType.DERIVED, schemaTO.getKey(), schemaTO);
    }

    public VirSchemaTO deleteVirSchema(final AttributableType type, final String name) {
        VirSchemaTO schemaTO = getService(SchemaService.class).read(type, SchemaType.VIRTUAL, name);
        getService(SchemaService.class).delete(type, SchemaType.VIRTUAL, name);
        return schemaTO;
    }

    public List<String> getAllValidatorClasses() {
        List<String> response = null;

        try {
            response = SyncopeConsoleSession.get().getSyncopeTO().getValidators();
        } catch (SyncopeClientException e) {
            LOG.error("While getting all validators", e);
        }
        return response;
    }
}
