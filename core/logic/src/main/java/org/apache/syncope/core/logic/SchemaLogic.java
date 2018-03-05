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
import java.util.function.Function;
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
import org.apache.syncope.common.lib.types.StandardEntitlement;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class SchemaLogic extends AbstractTransactionalLogic<SchemaTO> {

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private SchemaDataBinder binder;

    private boolean doesSchemaExist(final SchemaType schemaType, final String name) {
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

    @PreAuthorize("hasRole('" + StandardEntitlement.SCHEMA_CREATE + "')")
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
                created = (T) binder.getVirSchemaTO(virSchema);
                break;

            case DERIVED:
                DerSchema derSchema = derSchemaDAO.save(binder.create((DerSchemaTO) schemaTO));
                created = (T) binder.getDerSchemaTO(derSchema);
                break;

            case PLAIN:
            default:
                PlainSchema plainSchema = plainSchemaDAO.save(binder.create((PlainSchemaTO) schemaTO));
                created = (T) binder.getPlainSchemaTO(plainSchema);
        }
        return created;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.SCHEMA_DELETE + "')")
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
    @SuppressWarnings({ "unchecked", "Convert2Lambda" })
    public <T extends SchemaTO> List<T> list(
            final SchemaType schemaType, final List<String> anyTypeClasses) {
        return doSearch(schemaType, anyTypeClasses, null);
    }

    @PreAuthorize("isAuthenticated()")
    @SuppressWarnings("unchecked")
    public <T extends SchemaTO> List<T> search(
            final SchemaType schemaType, final List<String> anyTypeClasses, final String keyword) {
        return doSearch(schemaType, anyTypeClasses,
                keyword != null
                        ? StringUtils.replaceChars(keyword, "*", "%")
                        : null);
    }

    private <T extends SchemaTO> List<T> doSearch(
            final SchemaType schemaType, final List<String> anyTypeClasses, final String keyword) {
        List<AnyTypeClass> classes = new ArrayList<>(anyTypeClasses == null ? 0 : anyTypeClasses.size());
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
                        ? (keyword == null ? virSchemaDAO.findAll() : virSchemaDAO.search(keyword))
                        : virSchemaDAO.findByAnyTypeClasses(classes)).
                        stream().map(new Function<VirSchema, T>() {

                            @Override
                            public T apply(final VirSchema schema) {
                                return (T) binder.getVirSchemaTO(schema);
                            }
                        }).collect(Collectors.toList());
                break;

            case DERIVED:
                result = (classes.isEmpty()
                        ? (keyword == null ? derSchemaDAO.findAll() : derSchemaDAO.search(keyword))
                        : derSchemaDAO.findByAnyTypeClasses(classes)).
                        stream().map(new Function<DerSchema, T>() {

                            @Override
                            public T apply(final DerSchema schema) {
                                return (T) binder.getDerSchemaTO(schema);
                            }
                        }).collect(Collectors.toList());
                break;

            case PLAIN:
            default:
                result = (classes.isEmpty()
                        ? (keyword == null ? plainSchemaDAO.findAll() : plainSchemaDAO.search(keyword))
                        : plainSchemaDAO.findByAnyTypeClasses(classes)).
                        stream().map(new Function<PlainSchema, T>() {

                            @Override
                            public T apply(final PlainSchema schema) {
                                return (T) binder.getPlainSchemaTO(schema);
                            }
                        }).collect(Collectors.toList());
        }

        return result;
    }

    @PreAuthorize("isAuthenticated()")
    @SuppressWarnings("unchecked")
    public <T extends SchemaTO> T read(final SchemaType schemaType, final String schemaKey) {
        T read;
        switch (schemaType) {
            case VIRTUAL:
                VirSchema virSchema = virSchemaDAO.find(schemaKey);
                if (virSchema == null) {
                    throw new NotFoundException("Virtual Schema '" + schemaKey + "'");
                }

                read = (T) binder.getVirSchemaTO(virSchema);
                break;

            case DERIVED:
                DerSchema derSchema = derSchemaDAO.find(schemaKey);
                if (derSchema == null) {
                    throw new NotFoundException("Derived schema '" + schemaKey + "'");
                }

                read = (T) binder.getDerSchemaTO(derSchema);
                break;

            case PLAIN:
            default:
                PlainSchema schema = plainSchemaDAO.find(schemaKey);
                if (schema == null) {
                    throw new NotFoundException("Schema '" + schemaKey + "'");
                }

                read = (T) binder.getPlainSchemaTO(schema);
        }

        return read;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.SCHEMA_UPDATE + "')")
    public <T extends SchemaTO> void update(final SchemaType schemaType, final T schemaTO) {
        if (!doesSchemaExist(schemaType, schemaTO.getKey())) {
            throw new NotFoundException(schemaType + "/" + schemaTO.getKey());
        }

        switch (schemaType) {
            case VIRTUAL:
                VirSchema virSchema = virSchemaDAO.find(schemaTO.getKey());
                if (virSchema == null) {
                    throw new NotFoundException("Virtual Schema '" + schemaTO.getKey() + "'");
                }

                virSchemaDAO.save(binder.update((VirSchemaTO) schemaTO, virSchema));
                break;

            case DERIVED:
                DerSchema derSchema = derSchemaDAO.find(schemaTO.getKey());
                if (derSchema == null) {
                    throw new NotFoundException("Derived schema '" + schemaTO.getKey() + "'");
                }

                derSchemaDAO.save(binder.update((DerSchemaTO) schemaTO, derSchema));
                break;

            case PLAIN:
            default:
                PlainSchema plainSchema = plainSchemaDAO.find(schemaTO.getKey());
                if (plainSchema == null) {
                    throw new NotFoundException("Schema '" + schemaTO.getKey() + "'");
                }

                plainSchemaDAO.save(binder.update((PlainSchemaTO) schemaTO, plainSchema));
        }
    }

    @Override
    protected SchemaTO resolveReference(final Method method, final Object... args)
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
                            result = binder.getVirSchemaTO(virSchema);
                        }
                    } else {
                        result = binder.getDerSchemaTO(derSchema);
                    }
                } else {
                    result = binder.getPlainSchemaTO(plainSchema);
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
