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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.DropdownValueProvider;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class SchemaLogic extends AbstractTransactionalLogic<SchemaTO> {

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final ImplementationDAO implementationDAO;

    protected final SchemaDataBinder binder;

    protected final Map<String, DropdownValueProvider> perContextDropdownValueProviders = new ConcurrentHashMap<>();

    public SchemaLogic(
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final ImplementationDAO implementationDAO,
            final SchemaDataBinder binder) {

        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.implementationDAO = implementationDAO;
        this.binder = binder;
    }

    @SuppressWarnings("unchecked")
    protected <S extends Schema> Optional<S> findById(final SchemaType schemaType, final String name) {
        Optional<S> result = Optional.empty();

        switch (schemaType) {
            case VIRTUAL:
                result = (Optional<S>) virSchemaDAO.findById(name);
                break;

            case DERIVED:
                result = (Optional<S>) derSchemaDAO.findById(name);
                break;

            case PLAIN:
                result = (Optional<S>) plainSchemaDAO.findById(name);
                break;

            default:
        }

        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.SCHEMA_CREATE + "')")
    @SuppressWarnings("unchecked")
    public <T extends SchemaTO> T create(final SchemaType schemaType, final T schemaTO) {
        if (StringUtils.isBlank(schemaTO.getKey())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("Schema key");
            throw sce;
        }

        if (findById(schemaType, schemaTO.getKey()).isPresent()) {
            throw new DuplicateException(schemaType + "/" + schemaTO.getKey());
        }

        T created;
        switch (schemaType) {
            case VIRTUAL:
                created = (T) binder.getVirSchemaTO(binder.create((VirSchemaTO) schemaTO).getKey());
                break;

            case DERIVED:
                created = (T) binder.getDerSchemaTO(binder.create((DerSchemaTO) schemaTO).getKey());
                break;

            case PLAIN:
            default:
                created = (T) binder.getPlainSchemaTO(binder.create((PlainSchemaTO) schemaTO).getKey());
        }
        return created;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.SCHEMA_DELETE + "')")
    public void delete(final SchemaType schemaType, final String schemaKey) {
        findById(schemaType, schemaKey).
                orElseThrow(() -> new NotFoundException(schemaType + ": " + schemaKey));

        switch (schemaType) {
            case VIRTUAL:
                virSchemaDAO.deleteById(schemaKey);
                break;

            case DERIVED:
                derSchemaDAO.deleteById(schemaKey);
                break;

            case PLAIN:
            default:
                plainSchemaDAO.deleteById(schemaKey);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T extends SchemaTO> List<T> search(
            final SchemaType schemaType, final List<String> anyTypeClasses, final String keyword) {

        List<AnyTypeClass> classes = new ArrayList<>(anyTypeClasses.size());
        anyTypeClasses.remove(AnyTypeKind.USER.name());
        anyTypeClasses.remove(AnyTypeKind.GROUP.name());
        anyTypeClasses.forEach(anyTypeClass -> anyTypeClassDAO.findById(anyTypeClass).
                ifPresentOrElse(
                        classes::add,
                        () -> LOG.warn("Ignoring invalid {}: {}",
                                AnyTypeClass.class.getSimpleName(), anyTypeClass)));

        List<T> result;
        switch (schemaType) {
            case VIRTUAL:
                List<? extends VirSchema> virSchemas = classes.isEmpty()
                        ? keyword == null
                                ? virSchemaDAO.findAll()
                                : virSchemaDAO.findByIdLike(keyword)
                        : virSchemaDAO.findByAnyTypeClasses(classes);
                result = virSchemas.stream().map(schema -> (T) binder.getVirSchemaTO(schema.getKey())).
                        toList();
                break;

            case DERIVED:
                List<? extends DerSchema> derSchemas = classes.isEmpty()
                        ? keyword == null
                                ? derSchemaDAO.findAll()
                                : derSchemaDAO.findByIdLike(keyword)
                        : derSchemaDAO.findByAnyTypeClasses(classes);
                result = derSchemas.stream().map(schema -> (T) binder.getDerSchemaTO(schema.getKey())).
                        toList();
                break;

            case PLAIN:
            default:
                List<? extends PlainSchema> plainSchemas = classes.isEmpty()
                        ? keyword == null
                                ? plainSchemaDAO.findAll()
                                : plainSchemaDAO.findByIdLike(keyword)
                        : plainSchemaDAO.findByAnyTypeClasses(classes);
                result = plainSchemas.stream().map(schema -> (T) binder.getPlainSchemaTO(schema.getKey())).
                        toList();
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
        Schema schema = findById(schemaType, schemaTO.getKey()).
                orElseThrow(() -> new NotFoundException(schemaType + ": " + schemaTO.getKey()));

        switch (schemaType) {
            case VIRTUAL ->
                binder.update((VirSchemaTO) schemaTO, (VirSchema) schema);

            case DERIVED ->
                binder.update((DerSchemaTO) schemaTO, (DerSchema) schema);

            case PLAIN ->
                binder.update((PlainSchemaTO) schemaTO, (PlainSchema) schema);

            default -> {
            }
        }
    }

    @PreAuthorize("isAuthenticated()")
    public Attr getDropdownValues(final String key, final AnyTO anyTO) {
        PlainSchema schema = plainSchemaDAO.findById(key).
                filter(s -> s.getType() == AttrSchemaType.Dropdown).
                orElseThrow(() -> new NotFoundException(AttrSchemaType.Dropdown.name() + " PlainSchema " + key));

        try {
            DropdownValueProvider provider = ImplementationManager.build(
                    schema.getDropdownValueProvider(),
                    () -> perContextDropdownValueProviders.get(schema.getDropdownValueProvider().getKey()),
                    instance -> perContextDropdownValueProviders.put(
                            schema.getDropdownValueProvider().getKey(), instance));
            return new Attr.Builder(schema.getKey()).values(provider.getChoices(anyTO)).build();
        } catch (Exception e) {
            LOG.error("While getting dropdown values for {}", key, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidImplementation);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    @Override
    protected SchemaTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof SchemaTO schemaTO) {
                    key = schemaTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                SchemaTO result = null;

                Optional<? extends Schema> schema = plainSchemaDAO.findById(key);
                if (schema.isEmpty()) {
                    schema = derSchemaDAO.findById(key);
                    if (schema.isEmpty()) {
                        schema = virSchemaDAO.findById(key);
                        if (schema.isPresent()) {
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
