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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class SchemaLogic extends AbstractTransactionalLogic<AbstractSchemaTO> {

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private SchemaDataBinder binder;

    @Autowired
    private AttributableUtilsFactory attrUtilsFactory;

    private boolean doesSchemaExist(final SchemaType schemaType, final String name, final AttributableUtils attrUtils) {
        boolean found;

        switch (schemaType) {
            case VIRTUAL:
                found = virSchemaDAO.find(name, attrUtils.virSchemaClass()) != null;
                break;

            case DERIVED:
                found = derSchemaDAO.find(name, attrUtils.derSchemaClass()) != null;
                break;

            case PLAIN:
                found = plainSchemaDAO.find(name, attrUtils.plainSchemaClass()) != null;
                break;

            default:
                found = false;
        }

        return found;
    }

    @PreAuthorize("hasRole('" + Entitlement.SCHEMA_CREATE + "')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractSchemaTO> T create(
            final AttributableType attrType, final SchemaType schemaType, final T schemaTO) {

        if (StringUtils.isBlank(schemaTO.getKey())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("Schema name");
            throw sce;
        }

        AttributableUtils attrUtils = attrUtilsFactory.getInstance(attrType);
        if (doesSchemaExist(schemaType, schemaTO.getKey(), attrUtils)) {
            throw new DuplicateException(attrType + "/" + schemaType + "/" + schemaTO.getKey());
        }

        T created;
        switch (schemaType) {
            case VIRTUAL:
                VirSchema virSchema = attrUtils.newVirSchema();
                binder.create((VirSchemaTO) schemaTO, virSchema);
                virSchema = virSchemaDAO.save(virSchema);
                created = (T) binder.getVirSchemaTO(virSchema);
                break;
            case DERIVED:
                DerSchema derSchema = attrUtils.newDerSchema();
                binder.create((DerSchemaTO) schemaTO, derSchema);
                derSchema = derSchemaDAO.save(derSchema);

                created = (T) binder.getDerSchemaTO(derSchema);
                break;

            case PLAIN:
            default:
                PlainSchema normalSchema = attrUtils.newPlainSchema();
                binder.create((PlainSchemaTO) schemaTO, normalSchema);
                normalSchema = plainSchemaDAO.save(normalSchema);

                created = (T) binder.getPlainSchemaTO(normalSchema, attrUtils);
        }
        return created;
    }

    @PreAuthorize("hasRole('" + Entitlement.SCHEMA_DELETE + "')")
    public void delete(final AttributableType attrType, final SchemaType schemaType, final String schemaName) {
        final AttributableUtils attrUtils = attrUtilsFactory.getInstance(attrType);

        if (!doesSchemaExist(schemaType, schemaName, attrUtils)) {
            throw new NotFoundException(schemaType + "/" + attrType + "/" + schemaName);
        }

        switch (schemaType) {
            case VIRTUAL:
                virSchemaDAO.delete(schemaName, attrUtils);
                break;

            case DERIVED:
                derSchemaDAO.delete(schemaName, attrUtils);
                break;

            case PLAIN:
            default:
                plainSchemaDAO.delete(schemaName, attrUtils);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @SuppressWarnings("unchecked")
    public <T extends AbstractSchemaTO> List<T> list(final AttributableType attrType, final SchemaType schemaType) {
        final AttributableUtils attrUtils = attrUtilsFactory.getInstance(attrType);

        List<T> result;
        switch (schemaType) {
            case VIRTUAL:
                result = CollectionUtils.collect(virSchemaDAO.findAll(attrUtils.virSchemaClass()),
                        new Transformer<VirSchema, T>() {

                            @Override
                            public T transform(final VirSchema input) {
                                return (T) binder.getVirSchemaTO(input);
                            }
                        }, new ArrayList<T>());
                break;

            case DERIVED:
                result = CollectionUtils.collect(derSchemaDAO.findAll(attrUtils.derSchemaClass()),
                        new Transformer<DerSchema, T>() {

                            @Override
                            public T transform(final DerSchema input) {
                                return (T) binder.getDerSchemaTO(input);
                            }
                        }, new ArrayList<T>());
                break;

            case PLAIN:
            default:
                result = CollectionUtils.collect(plainSchemaDAO.findAll(attrUtils.plainSchemaClass()),
                        new Transformer<PlainSchema, T>() {

                            @Override
                            public T transform(final PlainSchema input) {
                                return (T) binder.getPlainSchemaTO(input, attrUtils);
                            }
                        }, new ArrayList<T>());
        }

        return result;
    }

    @PreAuthorize("hasRole('" + Entitlement.SCHEMA_READ + "')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractSchemaTO> T read(
            final AttributableType attrType, final SchemaType schemaType, final String schemaName) {

        final AttributableUtils attrUtils = attrUtilsFactory.getInstance(attrType);

        T read;
        switch (schemaType) {
            case VIRTUAL:
                VirSchema virSchema = virSchemaDAO.find(schemaName, attrUtils.virSchemaClass());
                if (virSchema == null) {
                    throw new NotFoundException("Virtual Schema '" + schemaName + "'");
                }

                read = (T) binder.getVirSchemaTO(virSchema);
                break;

            case DERIVED:
                DerSchema derSchema = derSchemaDAO.find(schemaName, attrUtils.derSchemaClass());
                if (derSchema == null) {
                    throw new NotFoundException("Derived schema '" + schemaName + "'");
                }

                read = (T) binder.getDerSchemaTO(derSchema);
                break;

            case PLAIN:
            default:
                PlainSchema schema = plainSchemaDAO.find(schemaName, attrUtils.plainSchemaClass());
                if (schema == null) {
                    throw new NotFoundException("Schema '" + schemaName + "'");
                }

                read = (T) binder.getPlainSchemaTO(schema, attrUtils);
        }

        return read;
    }

    @PreAuthorize("hasRole('" + Entitlement.SCHEMA_UPDATE + "')")
    public <T extends AbstractSchemaTO> void update(
            final AttributableType attrType, final SchemaType schemaType, final T schemaTO) {

        final AttributableUtils attrUtils = attrUtilsFactory.getInstance(attrType);

        if (!doesSchemaExist(schemaType, schemaTO.getKey(), attrUtils)) {
            throw new NotFoundException(schemaType + "/" + attrType + "/" + schemaTO.getKey());
        }

        switch (schemaType) {
            case VIRTUAL:
                VirSchema virSchema = virSchemaDAO.find(schemaTO.getKey(), attrUtils.virSchemaClass());
                if (virSchema == null) {
                    throw new NotFoundException("Virtual Schema '" + schemaTO.getKey() + "'");
                }

                binder.update((VirSchemaTO) schemaTO, virSchema);
                virSchemaDAO.save(virSchema);
                break;

            case DERIVED:
                DerSchema derSchema = derSchemaDAO.find(schemaTO.getKey(), attrUtils.derSchemaClass());
                if (derSchema == null) {
                    throw new NotFoundException("Derived schema '" + schemaTO.getKey() + "'");
                }

                binder.update((DerSchemaTO) schemaTO, derSchema);
                derSchemaDAO.save(derSchema);
                break;

            case PLAIN:
            default:
                PlainSchema schema = plainSchemaDAO.find(schemaTO.getKey(), attrUtils.plainSchemaClass());
                if (schema == null) {
                    throw new NotFoundException("Schema '" + schemaTO.getKey() + "'");
                }

                binder.update((PlainSchemaTO) schemaTO, schema, attrUtils);
                plainSchemaDAO.save(schema);
        }
    }

    @Override
    protected AbstractSchemaTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String kind = null;
        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; (key == null || kind == null) && i < args.length; i++) {
                if (args[i] instanceof String) {
                    if (kind == null) {
                        kind = (String) args[i];
                    } else {
                        key = (String) args[i];
                    }
                } else if (args[i] instanceof AbstractSchemaTO) {
                    key = ((AbstractSchemaTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                final AttributableUtils attrUtils = attrUtilsFactory.getInstance(kind);

                AbstractSchemaTO result = null;

                PlainSchema plainSchema = plainSchemaDAO.find(key, attrUtils.plainSchemaClass());
                if (plainSchema == null) {
                    DerSchema derSchema = derSchemaDAO.find(key, attrUtils.derSchemaClass());
                    if (derSchema == null) {
                        VirSchema virSchema = virSchemaDAO.find(key, attrUtils.virSchemaClass());
                        if (virSchema != null) {
                            result = binder.getVirSchemaTO(virSchema);
                        }
                    } else {
                        result = binder.getDerSchemaTO(derSchema);
                    }
                } else {
                    result = binder.getPlainSchemaTO(plainSchema, attrUtils);
                }

                return result;
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
