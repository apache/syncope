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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class SchemaLogic extends AbstractTransactionalLogic<SchemaTO> {

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final SchemaDataBinder binder;

    public SchemaLogic(
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final SchemaDataBinder binder) {

        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.binder = binder;
    }

    protected boolean doesSchemaExist(final SchemaType schemaType, final String name) {
        boolean found;

        switch (schemaType) {
            case VIRTUAL:
                found = virSchemaDAO.find(name) != null;
                break;

            case DERIVED:
                found = derSchemaDAO.find(name) != null;
                break;

            case PLAIN:
                found = plainSchemaDAO.find(name) != null;
                break;

            default:
                found = false;
        }

        return found;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.SCHEMA_CREATE + "')")
    @SuppressWarnings("unchecked")
    public <T extends SchemaTO> T create(final SchemaType schemaType, final T schemaTO) {
        if (StringUtils.isBlank(schemaTO.getKey())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("Schema key");
            throw sce;
        }

        if (doesSchemaExist(schemaType, schemaTO.getKey())) {
            throw new DuplicateException(schemaType + "/" + schemaTO.getKey());
        }

        T created;
        switch (schemaType) {
            case VIRTUAL:
                VirSchema virSchema = virSchemaDAO.save(binder.create((VirSchemaTO) schemaTO));
                created = (T) binder.getVirSchemaTO(virSchema.getKey());
                break;

            case DERIVED:
                DerSchema derSchema = derSchemaDAO.save(binder.create((DerSchemaTO) schemaTO));
                created = (T) binder.getDerSchemaTO(derSchema.getKey());
                break;

            case PLAIN:
            default:
                PlainSchema plainSchema = plainSchemaDAO.save(binder.create((PlainSchemaTO) schemaTO));
                created = (T) binder.getPlainSchemaTO(plainSchema.getKey());
        }
        return created;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.SCHEMA_DELETE + "')")
    public void delete(final SchemaType schemaType, final String schemaKey) {
        if (!doesSchemaExist(schemaType, schemaKey)) {
            throw new NotFoundException(schemaType + "/" + schemaKey);
        }

        switch (schemaType) {
            case VIRTUAL:
                virSchemaDAO.delete(schemaKey);
                break;

            case DERIVED:
                derSchemaDAO.delete(schemaKey);
                break;

            case PLAIN:
            default:
                plainSchemaDAO.delete(schemaKey);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T extends SchemaTO> List<T> search(
            final SchemaType schemaType, final List<String> anyTypeClasses, final String keyword) {

        List<AnyTypeClass> classes = new ArrayList<>(Optional.ofNullable(anyTypeClasses).map(List::size).orElse(0));
        if (anyTypeClasses != null) {
            anyTypeClasses.remove(AnyTypeKind.USER.name());
            anyTypeClasses.remove(AnyTypeKind.GROUP.name());
            anyTypeClasses.forEach(anyTypeClass -> {
                AnyTypeClass clazz = anyTypeClassDAO.find(anyTypeClass);
                if (clazz == null) {
                    LOG.warn("Ignoring invalid {}: {}", AnyTypeClass.class.getSimpleName(), anyTypeClass);
                } else {
                    classes.add(clazz);
                }
            });
        }

        List<T> result;
        switch (schemaType) {
            case VIRTUAL:
                result = (classes.isEmpty()
                        ? keyword == null
                                ? virSchemaDAO.findAll()
                                : virSchemaDAO.findByKeyword(keyword)
                        : virSchemaDAO.findByAnyTypeClasses(classes)).stream().
                        map(schema -> (T) binder.getVirSchemaTO(schema.getKey())).collect(Collectors.toList());
                break;

            case DERIVED:
                result = (classes.isEmpty()
                        ? keyword == null
                                ? derSchemaDAO.findAll()
                                : derSchemaDAO.findByKeyword(keyword)
                        : derSchemaDAO.findByAnyTypeClasses(classes)).stream().
                        map(schema -> (T) binder.getDerSchemaTO(schema.getKey())).collect(Collectors.toList());
                break;

            case PLAIN:
            default:
                result = (classes.isEmpty()
                        ? keyword == null
                                ? plainSchemaDAO.findAll()
                                : plainSchemaDAO.findByKeyword(keyword)
                        : plainSchemaDAO.findByAnyTypeClasses(classes)).stream().
                        map(schema -> (T) binder.getPlainSchemaTO(schema.getKey())).collect(Collectors.toList());
        }

        return result;
    }

    @PreAuthorize("isAuthenticated()")
    @SuppressWarnings("unchecked")
    public <T extends SchemaTO> T read(final SchemaType schemaType, final String schemaKey) {
        T read;
        switch (schemaType) {
            case VIRTUAL:
                read = (T) binder.getVirSchemaTO(schemaKey);
                break;

            case DERIVED:
                read = (T) binder.getDerSchemaTO(schemaKey);
                break;

            case PLAIN:
            default:
                read = (T) binder.getPlainSchemaTO(schemaKey);
        }

        return read;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.SCHEMA_UPDATE + "')")
    public <T extends SchemaTO> void update(final SchemaType schemaType, final T schemaTO) {
        if (!doesSchemaExist(schemaType, schemaTO.getKey())) {
            throw new NotFoundException(schemaType + "/" + schemaTO.getKey());
        }

        switch (schemaType) {
            case VIRTUAL:
                VirSchema virSchema = virSchemaDAO.find(schemaTO.getKey());
                if (virSchema == null) {
                    throw new NotFoundException("Virtual Schema '" + schemaTO.getKey() + '\'');
                }

                virSchemaDAO.save(binder.update((VirSchemaTO) schemaTO, virSchema));
                break;

            case DERIVED:
                DerSchema derSchema = derSchemaDAO.find(schemaTO.getKey());
                if (derSchema == null) {
                    throw new NotFoundException("Derived schema '" + schemaTO.getKey() + '\'');
                }

                derSchemaDAO.save(binder.update((DerSchemaTO) schemaTO, derSchema));
                break;

            case PLAIN:
            default:
                PlainSchema plainSchema = plainSchemaDAO.find(schemaTO.getKey());
                if (plainSchema == null) {
                    throw new NotFoundException("Schema '" + schemaTO.getKey() + '\'');
                }

                plainSchemaDAO.save(binder.update((PlainSchemaTO) schemaTO, plainSchema));
        }
    }

    @Override
    protected SchemaTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof SchemaTO) {
                    key = ((SchemaTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                SchemaTO result = null;

                PlainSchema plainSchema = plainSchemaDAO.find(key);
                if (plainSchema == null) {
                    DerSchema derSchema = derSchemaDAO.find(key);
                    if (derSchema == null) {
                        VirSchema virSchema = virSchemaDAO.find(key);
                        if (virSchema != null) {
                            result = binder.getVirSchemaTO(key);
                        }
                    } else {
                        result = binder.getDerSchemaTO(key);
                    }
                } else {
                    result = binder.getPlainSchemaTO(key);
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
