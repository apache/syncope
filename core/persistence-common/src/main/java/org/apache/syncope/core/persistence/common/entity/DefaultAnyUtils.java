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
package org.apache.syncope.core.persistence.common.entity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class DefaultAnyUtils implements AnyUtils {

    protected static final Logger LOG = LoggerFactory.getLogger(AnyUtils.class);

    protected static void initFieldNames(final Class<?> entityClass, final Map<String, Field> fields) {
        List<Class<?>> classes = ClassUtils.getAllSuperclasses(entityClass);
        classes.add(entityClass);
        classes.forEach(clazz -> {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())
                        && !field.getName().startsWith("pc")
                        && !Collection.class.isAssignableFrom(field.getType())
                        && !Map.class.isAssignableFrom(field.getType())) {

                    fields.put(field.getName(), field);
                    if ("id".equals(field.getName())) {
                        fields.put("key", field);
                    }
                }
            }
        });
    }

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final EntityFactory entityFactory;

    protected final AnyTypeKind anyTypeKind;

    protected final Map<String, Field> userFields = new HashMap<>();

    protected final Map<String, Field> groupFields = new HashMap<>();

    protected final Map<String, Field> anyObjectFields = new HashMap<>();

    public DefaultAnyUtils(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final AnyTypeKind anyTypeKind) {

        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.entityFactory = entityFactory;
        this.anyTypeKind = anyTypeKind;

        initFieldNames(entityFactory.userClass(), userFields);
        initFieldNames(entityFactory.groupClass(), groupFields);
        initFieldNames(entityFactory.anyObjectClass(), anyObjectFields);
    }

    @Override
    public AnyTypeKind anyTypeKind() {
        return anyTypeKind;
    }

    @Override
    public <T extends Any> Class<T> anyClass() {
        Class result;

        switch (anyTypeKind) {
            case GROUP:
                result = entityFactory.groupClass();
                break;

            case ANY_OBJECT:
                result = entityFactory.anyObjectClass();
                break;

            case USER:
            default:
                result = entityFactory.userClass();
        }

        return result;
    }

    @Override
    public Optional<Field> getField(final String name) {
        Map<String, Field> fields;

        switch (anyTypeKind) {
            case GROUP:
                fields = groupFields;
                break;

            case ANY_OBJECT:
                fields = anyObjectFields;
                break;

            case USER:
            default:
                fields = userFields;
                break;
        }

        return Optional.ofNullable(fields.get(name));
    }

    @Override
    public <T extends AnyTO> T newAnyTO() {
        T result = null;

        switch (anyTypeKind) {
            case USER:
                result = (T) new UserTO();
                break;

            case GROUP:
                result = (T) new GroupTO();
                break;

            case ANY_OBJECT:
                result = (T) new AnyObjectTO();
                break;

            default:
        }

        return result;
    }

    @Override
    public <C extends AnyCR> C newAnyCR() {
        C result = null;

        switch (anyTypeKind) {
            case USER:
                result = (C) new UserCR();
                break;

            case GROUP:
                result = (C) new GroupCR();
                break;

            case ANY_OBJECT:
                result = (C) new AnyObjectCR();
                break;

            default:
        }

        return result;
    }

    @Override
    public <U extends AnyUR> U newAnyUR(final String key) {
        U result = null;

        switch (anyTypeKind) {
            case USER:
                result = (U) new UserUR();
                break;

            case GROUP:
                result = (U) new GroupUR();
                break;

            case ANY_OBJECT:
                result = (U) new AnyObjectUR();
                break;

            default:
        }

        if (result != null) {
            result.setKey(key);
        }

        return result;
    }

    @Override
    public <A extends Any> AnyDAO<A> dao() {
        AnyDAO<A> result = null;

        switch (anyTypeKind) {
            case USER:
                result = (AnyDAO<A>) userDAO;
                break;

            case GROUP:
                result = (AnyDAO<A>) groupDAO;
                break;

            case ANY_OBJECT:
                result = (AnyDAO<A>) anyObjectDAO;
                break;

            default:
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Set<ExternalResource> getAllResources(final Any any) {
        Set<ExternalResource> resources = new HashSet<>();

        switch (any) {
            case User user ->
                resources.addAll(userDAO.findAllResources(user));

            case Group group ->
                resources.addAll(group.getResources());

            case AnyObject anyObject ->
                resources.addAll(anyObjectDAO.findAllResources(anyObject));

            default -> {
            }
        }

        return resources;
    }

    @Transactional
    @Override
    public void addAttr(
            final PlainAttrValidationManager validator,
            final String key,
            final PlainSchema schema,
            final String value) {

        Any any = dao().findById(key).orElseThrow(() -> new NotFoundException(anyTypeKind + " " + key));

        Set<AnyTypeClass> typeOwnClasses = new HashSet<>();
        typeOwnClasses.addAll(any.getType().getClasses());
        typeOwnClasses.addAll(any.getAuxClasses());
        if (typeOwnClasses.stream().noneMatch(clazz -> clazz.getPlainSchemas().contains(schema))) {
            LOG.warn("Schema {} not allowed for {}, ignoring", schema, any);
            return;
        }

        any.getPlainAttr(schema.getKey()).ifPresentOrElse(
                attr -> LOG.debug("{} has already {} set: {}", any, schema.getKey(), attr.getValuesAsStrings()),
                () -> {
                    PlainAttr attr = new PlainAttr();
                    attr.setPlainSchema(schema);
                    any.add(attr);

                    try {
                        attr.add(validator, value);
                        dao().save(any);
                    } catch (InvalidPlainAttrValueException e) {
                        LOG.error("Invalid value for attribute {} and {}: {}", schema.getKey(), any, value, e);
                    }
                });
    }

    @Transactional
    @Override
    public void removeAttr(final String key, final PlainSchema schema) {
        Any any = dao().findById(key).orElseThrow(() -> new NotFoundException(anyTypeKind + " " + key));

        any.getPlainAttr(schema.getKey()).ifPresentOrElse(
                attr -> {
                    any.remove(attr);
                    dao().save(any);
                },
                () -> LOG.warn("Any {} does not contain {} PLAIN attribute", key, schema.getKey()));
    }
}
